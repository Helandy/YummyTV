package su.afk.yummy.tv.source.anime4k.domain.model

/** Итог попытки открыть 4K-озвучку. Исключения транспорта сюда не попадают: они превращаются в [Failed]. */
sealed interface Anime4kStreamsResult {
    data class Success(val streams: Anime4kStreams) : Anime4kStreamsResult

    /** Пользователь отказался смотреть рекламу. */
    data object Declined : Anime4kStreamsResult

    /** Реклама не показана или награда не получена. */
    data object AdNotRewarded : Anime4kStreamsResult

    /** Сервер ограничил частоту запросов (429). */
    data object RateLimited : Anime4kStreamsResult

    /** Сервер отказал: тикет погашен раньше времени, просрочен или уже использован (403). */
    data object Rejected : Anime4kStreamsResult

    data object NotFound : Anime4kStreamsResult

    data class Failed(val reason: String?) : Anime4kStreamsResult
}
