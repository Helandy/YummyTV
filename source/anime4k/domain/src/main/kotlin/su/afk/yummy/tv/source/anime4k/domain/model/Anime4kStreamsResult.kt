package su.afk.yummy.tv.source.anime4k.domain.model

/** Итог попытки открыть 4K-озвучку. Исключения транспорта сюда не попадают: они превращаются в [Failed]. */
sealed interface Anime4kStreamsResult {
    data class Success(val streams: Anime4kStreams) : Anime4kStreamsResult

    /** Сервер ограничил частоту запросов (429). */
    data object RateLimited : Anime4kStreamsResult

    /** Сервер не принял подпись запроса (401/403): сборка не официальная или устарела. */
    data object Rejected : Anime4kStreamsResult

    data object NotFound : Anime4kStreamsResult

    data class Failed(val reason: String?) : Anime4kStreamsResult
}
