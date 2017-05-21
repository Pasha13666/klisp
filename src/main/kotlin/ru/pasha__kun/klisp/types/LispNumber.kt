package ru.pasha__kun.klisp.types

class LispNumber private constructor(private val value: Long, private val divider: Long): Number(), LispValue {
    companion object {
        fun make(value: Long, divider: Long): LispNumber {
            var a = value
            var b = divider
            if (b < 0){
                a = -a
                b = -b
            }
            if (b != 0L){
                val f = gcd(if(a < 0) -a else a, if (b < 0) -b else b)
                a /= f
                b /= f
            }
            return LispNumber(a, b)
        }

        tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

        operator fun invoke(value: Long) = LispNumber(value, 1)

        operator fun invoke(value: Double): LispNumber {
            var a = value
            var b = 0L

            while (a % 1.0 != 0.0){
                b *= 10
                a *= 10
            }

            return LispNumber.make(a.toLong(), b)
        }

        operator fun invoke(v: String): LispNumber {
            if (v.isEmpty()) return LispNumber(0, 1)

            when (v.toLowerCase()){
                "nan" -> return NAN
                "+inf", "+infinity", "inf", "infinity" -> return POSITIVE_INFINITY
                "-int", "-infinity" -> return NEGATIVE_INFINITY
            }

            var value = v
            val negative = when (value[0]){
                '-' -> {
                    value = v.substring(1)
                    true
                }
                '+' -> {
                    value = v.substring(1)
                    false
                }
                else -> false
            }

            var n = 0
            var a = 0L
            var b = 0L

            while (n < value.length && value[n] in '0'..'9'){
                a *= 10
                a += value[n++] - '0'
            }

            if (negative)
                a = -a

            if (n == value.length)
                return LispNumber(a, 1)

            else if (value[n] == '.') {
                n++

                while (n < value.length && value[n] in '0'..'9') {
                    a *= 10
                    b *= 10
                    a += value[n++] - '0'
                }

                return LispNumber.make(a, b)

            } else if (value[n] == '$')
                return LispNumber.make(a, value.substring(n + 1).toLong())
            else throw NumberFormatException(v)
        }

        val NAN = LispNumber(0, 0)
        val POSITIVE_INFINITY = LispNumber(1, 0)
        val NEGATIVE_INFINITY = LispNumber(-1, 0)
    }

    override fun toByte() = toShort().toByte()

    override fun toChar() = toShort().toChar()

    override fun toShort() = toInt().toShort()

    override fun toInt() = toLong().toInt()

    override fun toFloat() = toDouble().toFloat()

    override fun toDouble() = value.toDouble() / divider.toDouble()

    override fun toLong() = value / divider

    operator fun div(b: LispNumber) = make(value * b.divider, divider * b.value)

    operator fun times(b: LispNumber) = make(value * b.value, divider * b.divider)

    operator fun plus(b: LispNumber) = make(value * b.divider + b.value * divider, divider * b.divider)

    operator fun minus(b: LispNumber) = make(value * b.divider - b.value * divider, divider * b.divider)

    operator fun rem(b: LispNumber) = make(value * b.divider % b.value * divider, divider * b.divider)

    override fun toString() = when (divider){
        1L -> value.toString()
        0L -> when (value) {
            1L -> "+Inf"
            0L -> "NaN"
            -1L -> "-Inf"
            else -> "<invalid number $value/0>"
        }
        else -> toDouble().toString()
    }

    override fun boolean() = value != 0L
}