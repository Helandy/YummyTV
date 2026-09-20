package su.afk.yummy.tv.core.preferences.auth

/** Шифрование токена. Вынесено за интерфейс ради подмены в тестах и запасного режима. */
internal interface TokenCipher {
    val mode: TokenStorageMode

    fun encrypt(value: String): String

    fun decrypt(value: String): String

    /** Проверка «шифр вообще работает» — round-trip на заведомо валидном значении. */
    fun selfTest(): Boolean
}
