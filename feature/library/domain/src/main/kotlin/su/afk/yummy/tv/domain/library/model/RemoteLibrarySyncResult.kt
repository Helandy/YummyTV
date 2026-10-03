package su.afk.yummy.tv.domain.library.model

sealed interface RemoteLibrarySyncResult {
    /** Синхронизация завершена; [pushErrors] — ошибки отправки локальных изменений (по одной на каждую неудачу). */
    data class Success(val pushErrors: List<Throwable> = emptyList()) : RemoteLibrarySyncResult
    data class Failure(val error: Throwable) : RemoteLibrarySyncResult
}
