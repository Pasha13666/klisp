package ru.pasha__kun.klisp

/**
 * Токен парсера.
 */
sealed class Token {
    /**
     * Символ '('.
     */
    object OPEN_LIST : Token()

    /**
     * Символ ')'.
     */
    object CLOSE_LIST : Token()

    /**
     * Символ '['.
     */
    object OPEN_ARRAY : Token()

    /**
     * Символ ']'.
     */
    object CLOSE_ARRAY : Token()

    /**
     * Символ '{'.
     */
    object OPEN_MAP : Token()

    /**
     * Символ '}'.
     */
    object CLOSE_MAP : Token()

    /**
     * Символ '''.
     */
    object QUOTE : Token()

    /**
     * Числовое значение.
     */
    data class NUMBER(val value: String) : Token()

    /**
     * Строковое значение.
     */
    data class STRING(val value: String) : Token()

    /**
     * Символьное значение (имя).
     */
    data class NAME(val value: String) : Token()

    /**
     * Значение типа "Ключ".
     */
    data class KEY(val value: String) : Token()
}