package ru.pasha__kun.klisp.types

import ru.pasha__kun.klisp.Environment
import ru.pasha__kun.klisp.LispInterpreter

/**
 * Интерфейс, который реализует каждый объект klisp.
 */
interface LispValue {

    /**
     * Вызывается для выполнения объекта.
     *
     * @param strict Строгий вызов функций
     * @param env Окружение выполнения
     * @param inter Объект интерпретатора
     */
    fun eval(strict: Boolean, env: Environment, inter: LispInterpreter): LispValue = this

    /**
     * Вызывается при вызове объекта.
     *
     * (fun 1 2 3) //=> <fun>.call(LispList(<1 2 3>), strict, env, inter)
     * @param args Аргументы функции
     * @param strict Строгий вызов функций
     * @param env Окружение выполнения
     * @param inter Объект интерпретатора
     */
    fun call(args: LispList, strict: Boolean, env: Environment, inter: LispInterpreter): LispValue {
        var v = this
        args.forEach {
            v = it.eval(true, env, inter)
        }
        return v
    }

    fun boolean(): Boolean
}