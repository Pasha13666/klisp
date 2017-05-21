package ru.pasha__kun.klisp

import ru.pasha__kun.klisp.types.*
import java.io.FileReader
import java.io.Reader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.experimental.buildSequence

/**
 * Главный класс интерпретатора.
 */
open class LispInterpreter {
    companion object {
        val BUILTIN_NATIVE_MODULES = listOf<String>()
        val BUILTIN_NATIVE_TYPES = listOf<String>()
    }

    private enum class ParserState {
        LIST,
        ARRAY,
        MAP
    }

    /**
     * "Уровень" окружения - целое число от 0 до 100, определяющее функции стандартной библиотеки.
     *
     * 0 - Без стандартной библиотеки
     * 1 - Только базовый синтаксис (lambda, define, macro, set)
     * 2 - + Базовые функции (*, -, ++)
     * 3 - + Дополнительный синтаксис (циклы, условия)
     * 4 - + Конструкторы типов и import
     * 5 - + Стандартные библиотеки типов
     * 6 - + Базовые библиотеки (io, network)
     * 7 - + Библиотека нативных вызовов (call-java)
     * 8 - + Библиотека отладки (debug)
     * 9 - Автоимпорт всех нативных библиотек.
     * 10 - Автоимпорт всех библиотек из [path].
     */
    var environmentLevel = 10
        get
        set(value){
            if (value !in 0..10) throw IllegalArgumentException("environmentLevel must be in range 0..10!")
            synchronized(this){
                field = value
                _environment = null
                clearImportCache()
            }
        }

    /**
     * Пути пойска библиотек для импорта.
     */
    val path = ImportPath()

    /**
     * Пути пойска библиотек для импорта из classpath.
     */
    val internalPath: MutableList<String> = arrayListOf("ru/pasha__kun/klisp/library")

    /**
     * Пути пойска нативных библиотек для импорта из classpath.
     */
    val nativePath: MutableList<String> = arrayListOf("ru.pasha__kun.klisp.library")

    var async = false

    private var _environment: Environment? = null
    private val loaded = HashMap<String, LispMap>()

    /**
     * Очищает кеш импорта модулей.
     */
    fun clearImportCache(){
        loaded.clear()
    }

    /**
     * Создает новое корневое окружение.
     */
    fun newRootEnvironment(): Environment {
        val env = synchronized(this){
            if (_environment == null)
                _environment = makeEnvForLevel(environmentLevel)
            _environment!!
        }.makeChild()
        env["(exports)"] = LispMap()
        return env
    }

    /**
     * Импортирует модуль с имянем [name] в окружение [env] и возвращает таблицу его функций.
     *
     * @param name Имя модуля
     * @param env Окружение вызова
     * @param cache Включить кеширование при импорте
     */
    fun importModule(name: String, env: Environment, cache: Boolean = true): LispMap {
        if (cache) {
            val r = loaded[name]
            if (r != null) return env.loadFrom(r)
        }

        path.lookup(name)?.let {
            val env2 = newRootEnvironment()
            parse(tokenize(FileReader(it))).eval(false, env2, this)
            return (env2["(exports)"] as LispMap).also {
                if (cache) loaded[name] = it
            }
        }

        internalPath.forEach {
            val s = LispInterpreter::class.java.getResourceAsStream("/$it/$name.kl")
            if (s != null) {
                val env2 = newRootEnvironment()
                parse(tokenize(it.reader())).eval(false, env2, this)
                return (env2["(exports)"] as LispMap).also {
                    if (cache) loaded[name] = it
                }
            }
        }

        val cn = name2class(name)
        nativePath.forEach {
            val c = try { Class.forName("$it.$cn").newInstance() }
                catch (e: InstantiationException) { null }
                catch (e: IllegalAccessException) { null }
                catch (e: ClassNotFoundException) { null }
            if (c != null && c is LispNativeLibrary)
                return c.import(env).also {
                    if (cache) loaded[name] = it
                }
        }

        throw RuntimeException("No module named $name")
    }

    /**
     * Разбивает поток на токены. Метот разбития зависит от [async]
     */
    fun tokenize(input: Reader) = if (async) tokenizeAsync(input) else tokenizeSync(input)

    /**
     * Синхронно разбивает поток на токены.
     */
    open fun tokenizeSync(input: Reader): ArrayList<Token> {
        val res = ArrayList<Token>()
        var c = 0
        var re = true
        while (true){
            if (re) c = input.read()
            re = true
            when (c){
                -1 -> return res
                9, 10, 13, 32 -> {}
                40 -> res.add(Token.OPEN_LIST)
                41 -> res.add(Token.CLOSE_LIST)
                123 -> res.add(Token.OPEN_MAP)
                125 -> res.add(Token.CLOSE_MAP)
                91 -> res.add(Token.OPEN_ARRAY)
                93 -> res.add(Token.CLOSE_ARRAY)
                39 -> res.add(Token.QUOTE)
                34 -> res.add(Token.STRING(buildString {
                    while (true){
                        c = input.read()
                        if (c == -1) throw RuntimeException()
                        if (c == 34) break
                        append(c.toChar())
                    }
                }))
                58 -> res.add(Token.KEY(buildString {
                    c = input.read()
                    while (c in 48..57 || c in 97..122 || c in 65..90 || c == 95){
                        append(c.toChar())
                        c = input.read()
                    }
                    re = false
                }))
                in 48..57, 45 -> {
                    val v = buildString {
                        while (c in 48..57 || c in 45..46 || c == 101 || c == 69) {
                            append(c.toChar())
                            c = input.read()
                        }
                        re = false
                    }

                    if (v == "-") res.add(Token.NAME("-"))
                    else res.add(Token.NUMBER(v))
                }
                else -> res.add(Token.NAME(buildString {
                    while (c in 94..122 || c in 33..38 || c in 42..90 || c == 124 || c == 92 || c == 126) {
                        append(c.toChar())
                        c = input.read()
                    }
                    re = false
                }))
            }
        }
    }

    /**
     * Асинхронно разбивает поток на токены.
     */
    open fun tokenizeAsync(input: Reader) = object : Iterable<Token> {
        private val b = buildSequence {
            var c = 0
            var re = true
            while (true) {
                if (re) c = input.read()
                re = true
                when (c) {
                    -1 -> return@buildSequence
                    9, 10, 13, 32 -> {
                    }
                    40 -> yield(Token.OPEN_LIST)
                    41 -> yield(Token.CLOSE_LIST)
                    123 -> yield(Token.OPEN_MAP)
                    125 -> yield(Token.CLOSE_MAP)
                    91 -> yield(Token.OPEN_ARRAY)
                    93 -> yield(Token.CLOSE_ARRAY)
                    39 -> yield(Token.QUOTE)
                    34 -> yield(Token.STRING(buildString {
                        while (true) {
                            c = input.read()
                            if (c == -1) throw RuntimeException()
                            if (c == 34) break
                            append(c.toChar())
                        }
                    }))
                    58 -> yield(Token.KEY(buildString {
                        c = input.read()
                        while (c in 48..57 || c in 97..122 || c in 65..90 || c == 95) {
                            append(c.toChar())
                            c = input.read()
                        }
                        re = false
                    }))
                    in 48..57, 45 -> {
                        val v = buildString {
                            while (c in 48..57 || c in 45..46 || c == 101 || c == 69) {
                                append(c.toChar())
                                c = input.read()
                            }
                            re = false
                        }

                        if (v == "-") yield(Token.NAME("-"))
                        else yield(Token.NUMBER(v))
                    }
                    else -> yield(Token.NAME(buildString {
                        while (c in 94..122 || c in 33..38 || c in 42..90 || c == 124 || c == 92 || c == 126) {
                            append(c.toChar())
                            c = input.read()
                        }
                        re = false
                    }))
                }
            }
        }

        override fun iterator() = b.iterator()
    }

    /**
     * Парсит значение из токенов.
     * @param inp Токены
     */
    fun parse(inp: Iterable<Token>): LispValue {
        val input = inp.iterator()
        val stack = Stack<LispList>()
        val state = Stack<ParserState>()
        stack.push(LispList())
        state.push(ParserState.LIST)

        while (input.hasNext()) {
            val token = input.next()
            when (token) {
                is Token.OPEN_LIST -> {
                    state.push(ParserState.LIST)
                    stack.push(LispList())
                }
                is Token.CLOSE_LIST -> {
                    assert(state.pop() == ParserState.LIST)
                    val v = stack.pop()
                    stack.peek().add(v)
                }
                is Token.OPEN_ARRAY -> {
                    state.push(ParserState.ARRAY)
                    stack.push(LispList())
                }
                is Token.CLOSE_ARRAY -> {
                    assert(state.pop() == ParserState.ARRAY)
                    val v = stack.pop()
                    stack.peek().add(LispArray(v))
                }
                is Token.OPEN_MAP -> {
                    state.push(ParserState.MAP)
                    stack.push(LispList())
                }
                is Token.CLOSE_MAP -> {
                    assert(state.pop() == ParserState.MAP)
                    val v = stack.pop()
                    stack.peek().add(LispMap().apply {
                        val i = v.iterator()
                        while (i.hasNext()){
                            val k = i.next()
                            assert(i.hasNext() && k is LispKey)
                            put(k as LispKey, i.next())
                        }
                    })
                }
                is Token.QUOTE -> {}
                is Token.NUMBER -> stack.peek().add(LispNumber(token.value))
                is Token.STRING -> stack.peek().add(LispString(token.value))
                is Token.NAME -> stack.peek().add(LispName(token.value))
                is Token.KEY -> stack.peek().add(LispKey(token.value))
            }
        }

        assert(stack.size == 1)
        return stack.pop()
    }

    /**
     * Создает окружение для данного уровня [level].
     *
     * Переопределите этот метод при использовании собственного класса окружения.
     */
    protected fun makeEnvForLevel(level: Int): Environment {
        val e = Environment()
        e["(builtin_level)"] = LispNumber(level.toLong())
        if (level >= 1) importModule("__builtin__", e, cache = false)
        if (level >= 4) BUILTIN_NATIVE_TYPES.forEach { importModule(it, e) }
        // (level >= 5) // from upper.
        if (level >= 6) BUILTIN_NATIVE_MODULES.forEach { importModule(it, e) }
        if (level >= 7) importModule("call-java", e)
        if (level >= 8) importModule("debug", e)
        if (level >= 9) TODO("Native auto-import")
        if (level == 10) TODO("Full auto-import")

        return e
    }

    /**
     * Переводит имя модуля в имя класса для импорта нативного модуля.
     */
    protected fun name2class(name: String) = buildString {
        var up = true
        name.forEach {
            if (it == '_' || it == '-') up = !up
            else {
                append(if (up) it.toUpperCase() else it)
                up = false
            }
        }
        append("Library")
    }
}