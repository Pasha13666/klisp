package ru.pasha__kun.klisp

import ru.pasha__kun.klisp.types.LispMap
import ru.pasha__kun.klisp.types.LispString
import ru.pasha__kun.klisp.types.LispValue

/**
 * Класс окружения вызовов - иерархическая стругтура для хранения переменнх.
 */
class Environment private constructor(val parent: Environment?) : HashMap<String, LispValue>(){
    constructor() : this(null)

    init {
        assert(parent != this)
    }

    override fun get(key: String): LispValue = super.get(key) ?: parent?.get(key) ?: LispString(key)

    /**
     * Заменяет переменную с имянем [key] на значение [value] в текущем и родительских окружениях.
     *
     * Возвращает false если переменная не найдена, иначе true.
     */
    fun set(key: String, value: LispValue): Boolean{
        var v: Environment? = this
        while (v != null){
            if (key in v){
                v[key] = value
                return true
            }
            v = v.parent
        }
        return false
    }

    /**
     * Создает новое окружение, у которого [parent] == this.
     */
    fun makeChild() = Environment(this)

    fun loadFrom(m: LispMap): LispMap {
        m.forEach { k, v ->
            set(k.value, v)
        }
        return m
    }

    override fun toString() = buildString {
        append(super.toString())
        if (parent != null){
            append("\nFrom parent:\n")
            append(parent)
        }
    }
}
