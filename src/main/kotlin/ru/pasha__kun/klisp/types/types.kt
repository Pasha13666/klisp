package ru.pasha__kun.klisp.types

import ru.pasha__kun.klisp.Environment
import ru.pasha__kun.klisp.LispInterpreter
import ru.pasha__kun.klisp.macroExpand
import ru.pasha__kun.klisp.mapArgs

class LispArray(v: Collection<LispValue>) : ArrayList<LispValue>(v), LispValue {
    override fun boolean() = isNotEmpty()
}

class LispMap : HashMap<LispKey, LispValue>(), LispValue {
    override fun toString() = buildString {
        append('{')
        forEach { k, v ->
            append(k.value)
            append('=')
            append(v)
        }
        append('}')
    }
    override fun boolean() = isNotEmpty()
}

data class LispKey(val value: String) : LispValue {
    override fun toString() = ":$value"
    override fun boolean() = value.isNotEmpty()
}

data class LispString(val value: String) : LispValue {
    override fun toString() = "\"$value\""
    override fun boolean() = value.isNotEmpty()
}

data class LispName(val value: String) : LispValue {
    override fun eval(strict: Boolean, env: Environment, inter: LispInterpreter) = env[value]

    override fun toString() = value
    override fun boolean() = value.isNotEmpty()
}

class LispFunction(val args: LispArray, val body: LispValue, val closure: Environment) : LispValue {
    override fun call(args: LispList, strict: Boolean, env: Environment, inter: LispInterpreter): LispValue {
        val fc = LispCallState(this, mapArgs(this.args, args))

        if (strict){
            var v: LispValue = fc
            while (v is LispCallState){
                v = v.func.body.eval(false, v.func.closure.makeChild().also {
                    it.putAll((v as LispCallState).args)
                }, inter)
            }
            return v
        }
        return fc
    }

    override fun boolean() = true
}

class LispMacro(val args: LispArray, val body: LispValue) : LispValue {
    override fun call(args: LispList, strict: Boolean, env: Environment, inter: LispInterpreter)
            = macroExpand(mapArgs(this.args, args), body).eval(true, env, inter)

    override fun boolean() = true
}

class LispCallState(val func: LispFunction, val args: Map<String, LispValue>) : LispValue {
    override fun eval(strict: Boolean, env: Environment, inter: LispInterpreter): LispValue {
        var v: LispValue = this
        while (v is LispCallState)
            v = v.func.body.eval(false, v.func.closure.makeChild().also {
                it.putAll((v as LispCallState).args)
            }, inter)
        return v
    }

    override fun boolean() = true
}
