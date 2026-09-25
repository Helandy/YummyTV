package su.afk.yummy.tv.data.account.backup

/**
 * Резервная копия refresh-токена вне данных приложения — переживает переустановку и перенос
 * на новое устройство. Все операции best-effort: сбой хранилища не должен ломать вход.
 */
interface AuthTokenBackup {
    suspend fun save(token: String)

    suspend fun restore(): String?

    suspend fun clear()
}
