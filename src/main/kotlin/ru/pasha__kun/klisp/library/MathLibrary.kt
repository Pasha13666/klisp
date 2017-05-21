package ru.pasha__kun.klisp.library

import ru.pasha__kun.klisp.Environment
import ru.pasha__kun.klisp.LispInterpreter
import ru.pasha__kun.klisp.types.*

class MathLibrary : LispNativeLibrary() {
    override val name = "math"
    override val names = listOf("")
    override val isRaw = false

    override fun eval(name: LispName, args: LispList, env: Environment, inter: LispInterpreter) = when (name.value) {
        this.name -> this

        else -> throw RuntimeException("")
    }
}