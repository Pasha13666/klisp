
@file:JvmName("KlispMain")
package ru.pasha__kun

import ru.pasha__kun.klisp.LispInterpreter
import ru.pasha__kun.klisp.types.LispCallState

fun main(args: Array<String>){
    val int = LispInterpreter()
    int.environmentLevel = 5
    val env = int.newRootEnvironment()
    var res = int.parse(int.tokenize(System.`in`.reader())).eval(true, env, int)

    if (res is LispCallState)
        res = res.eval(true, env, int)

    println("result: $res")
    println(env)
}
