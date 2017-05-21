package ru.pasha__kun.klisp.types

import ru.pasha__kun.klisp.Environment
import ru.pasha__kun.klisp.LispInterpreter

/**
 * Абстрактный класс нативной библиотеки (на kotlin/java).
 */
abstract class LispNativeLibrary : LispValue {
    /**
     * Имя библиотеки для экспорта. null для глобальной библиотеки.
     */
    protected abstract val name: String?

    /**
     * Имена функций библиотеки.
     */
    protected abstract val names: List<String>

    /**
     * Если false аргументы будут выполнены перед вызовом, иначе будут переданы как есть.
     */
    protected abstract val isRaw: Boolean

    protected abstract fun eval(name: LispName, args: LispList, env: Environment, inter: LispInterpreter): LispValue

    /**
     * Вызывается при импорте библиотеки.
     *
     * Добавляет функции библиотеки в окружение и возвращает их в виде LispMap.
     * @param env Окружение вызова
     */
    open fun import(env: Environment): LispMap {
        val m = LispMap()
        names.forEach {
            (if (name != null) "$name.$it" else it).let {
                env[it] = this
                m[LispKey(it)] = this
            }
        }
        if (name != null) {
            env[name!!] = this
            m[LispKey(name!!)] = this
        }
        return m
    }

    override fun call(args: LispList, strict: Boolean, env: Environment, inter: LispInterpreter): LispValue {
        val r_args = LispList()
        if (isRaw) (1..args.lastIndex).forEach {
            r_args.add(args[it])
        }
        else (1..args.lastIndex).forEach {
            r_args.add(args[it].eval(strict, env, inter))
        }
        return eval(args[0] as? LispName ?: LispName("()"), r_args, env, inter)
    }

    override fun boolean() = true

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline fun argCount(args: LispList, count: Int)
                = if (count in args.indices) args else throw RuntimeException("Not enough arguments to call function!")

        /**
         * Возвращает аргумент, проверяя количество аргументов и их тип.
         */
        inline fun <reified T : LispValue> getArg(args: LispList, arg: Int): T = argCount(args, arg)[arg].let {
            it as? T ?: throw RuntimeException("Invalid argument type ($arg): excepted ${T::class.java.simpleName}, got ${it::class.java.simpleName}")
        }
    }
}