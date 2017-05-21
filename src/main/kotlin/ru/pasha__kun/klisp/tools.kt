package ru.pasha__kun.klisp

import ru.pasha__kun.klisp.types.*
import java.util.HashMap


internal fun macroExpand(args: Map<String, LispValue>, body: LispValue): LispValue = when(body){
    is LispName -> args.getOrDefault(body.value, body)
    is LispList -> LispList().apply {
        addAll(body.map { macroExpand(args, it) })
    }
    else -> body
}

internal fun mapArgs(names: LispArray, args: LispList): Map<String, LispValue> {
    val v = names.size
    val a = args.size - 1
    if (v > a) throw RuntimeException("Not enough arguments!")
    val r = HashMap<String, LispValue>()
    for (i in 0..v - 2) {
        r[names[i].run {
            when (this) {
                is LispString -> this.value
                is LispName -> this.value
                is LispKey -> this.value
                else -> throw RuntimeException("Bad argument name!")
            }
        }] = args[i + 1]
    }
    r[names[v - 1].run {
        when (this) {
            is LispString -> this.value
            is LispName -> this.value
            is LispKey -> this.value
            else -> throw RuntimeException("Bad argument name!")
        }
    }] = if (v < a) args.subList(v, a) as LispList else args[v]
    return r
}

inline fun<T> Iterable<T>.forEachFrom(index: Int, fn: (T)->Unit){
    forEachIndexed { i, v ->
        if (i >= index) fn(v)
    }
}