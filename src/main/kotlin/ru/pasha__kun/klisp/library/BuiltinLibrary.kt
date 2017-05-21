package ru.pasha__kun.klisp.library

import ru.pasha__kun.klisp.Environment
import ru.pasha__kun.klisp.LispInterpreter
import ru.pasha__kun.klisp.forEachFrom
import ru.pasha__kun.klisp.types.*

class BuiltinLibrary : LispNativeLibrary() {
    override val name = null
    override val names = emptyList<String>()
    override val isRaw = true

    override fun import(env: Environment): LispMap {
        val m = LispMap()
        val l = (env["(builtin_level)"] as LispNumber).toInt()
        if (l >= 1) {
            m[LispKey("lambda")] = this
            env["lambda"] = this
            m[LispKey("define")] = this
            env["define"] = this
            m[LispKey("macro")] = this
            env["macro"] = this
            m[LispKey("set")] = this
            env["set"] = this
            m[LispKey("begin")] = this
            env["begin"] = this
        }
        if (l >= 2) {
            m[LispKey("+")] = this
            env["+"] = this
            m[LispKey("-")] = this
            env["-"] = this
            m[LispKey("*")] = this
            env["*"] = this
            m[LispKey("/")] = this
            env["/"] = this
            m[LispKey("%")] = this
            env["%"] = this
            m[LispKey("^")] = this
            env["^"] = this
            m[LispKey("++")] = this
            env["++"] = this
            m[LispKey("=")] = this
            env["="] = this
            m[LispKey("!=")] = this
            env["!="] = this
            m[LispKey("&&")] = this
            env["&&"] = this
            m[LispKey("||")] = this
            env["||"] = this
            m[LispKey("!")] = this
            env["!"] = this
            m[LispKey(">")] = this
            env[">"] = this
            m[LispKey("<")] = this
            env["<"] = this
            m[LispKey(">=")] = this
            env[">="] = this
            m[LispKey("<=")] = this
            env["<="] = this
        }
        if (l >= 3) {
            m[LispKey("if")] = this
            env["if"] = this
            m[LispKey("loop")] = this
            env["loop"] = this
            m[LispKey("each")] = this
            env["each"] = this
        }
        if (l >= 4) {
            m[LispKey("import")] = this
            env["import"] = this
            m[LispKey("export")] = this
            env["export"] = this
        }
        return m
    }

    override fun call(args: LispList, strict: Boolean, env: Environment, inter: LispInterpreter)
            = when(((if (args.size != 0) args[0] else null) as? LispName)?.value) {
        "lambda" -> LispFunction(getArg<LispArray>(args, 1), getArg<LispValue>(args, 2), env)
        "macro" -> LispMacro(getArg<LispArray>(args, 1), getArg<LispValue>(args, 2))
        "define" -> getArg<LispValue>(args, 2).eval(true, env, inter).also {
            env[getArg<LispName>(args, 1).value] = it
        }
        "set" -> getArg<LispValue>(args, 2).eval(true, env, inter).also {
            env.set(getArg<LispName>(args, 1).value, it)
        }
        "import" -> {
            var r = args[0]
            args.forEachFrom(1) {
                r = inter.importModule((it as LispName).value, env)
            }
            r
        }
        "export" -> {
            var r = args[0]
            var i = 1
            while (i < args.size)
                r = env[(args[i++] as LispName).value].also {
                    (env["(exports)"] as LispMap)[args[i++].eval(strict, env, inter) as LispKey] = it
                }
            r
        }
        "begin" -> {
            var r = args[0]
            args.forEachIndexed { i, v -> // TODO: check 'strict'
                if (i != 0) r = v.eval(strict, env, inter)
            }
            r
        }

        "+" -> {
            var r = LispNumber(0)
            args.forEachFrom(1) { r += it.eval(true, env, inter) as LispNumber }
            r
        }
        "*" -> {
            var r = LispNumber(1)
            args.forEachFrom(1) { r *= it.eval(true, env, inter) as LispNumber }
            r
        }
        "-" -> args[1].eval(true, env, inter) as LispNumber - args[2].eval(true, env, inter) as LispNumber
        "/" -> args[1].eval(true, env, inter) as LispNumber / args[2].eval(true, env, inter) as LispNumber
        "%" -> args[1].eval(true, env, inter) as LispNumber % args[2].eval(true, env, inter) as LispNumber
        "^" -> LispNumber(Math.pow((args[1].eval(true, env, inter) as LispNumber).toDouble(),
                (args[2].eval(true, env, inter) as LispNumber).toDouble()))
        "++" -> LispString(buildString {
            args.forEach {
                append(it.eval(true, env, inter))
            }
        })

        "==" -> {
            val r = args[1].eval(true, env, inter)
            var f = true
            var i = 2
            while (i < args.size){
                val l = args[i++].eval(true, env, inter)
                if (r != l){
                    f = false
                    break
                }
            }
            LispNumber(if (f) 1L else 0L)
        }
        "!=" -> {
            val r = args[1].eval(true, env, inter)
            var f = true
            var i = 2
            while (i < args.size){
                val l = args[i++].eval(true, env, inter)
                if (r == l){
                    f = false
                    break
                }
            }
            LispNumber(if (f) 1L else 0L)
        }
        "&&" -> {
            var f = true
            var i = 1
            while (i < args.size){
                val l = args[i++].eval(true, env, inter)
                if (!l.boolean()){
                    f = false
                    break
                }
            }
            LispNumber(if (f) 1L else 0L)
        }
        "||" -> {
            var f = true
            var i = 1
            while (i < args.size){
                val l = args[i++].eval(true, env, inter)
                if (l.boolean()){
                    f = false
                    break
                }
            }
            LispNumber(if (f) 1L else 0L)
        }
        "!" -> LispNumber(if (args[1].eval(true, env, inter).boolean()) 0L else 1L)

        "<", ">", "<=", ">=" -> TODO("Compare operations")
        else -> throw RuntimeException("Invalid native renaming!")
    }

    override fun eval(name: LispName, args: LispList, env: Environment, inter: LispInterpreter)
            = throw Error("Internal broken method!")
}