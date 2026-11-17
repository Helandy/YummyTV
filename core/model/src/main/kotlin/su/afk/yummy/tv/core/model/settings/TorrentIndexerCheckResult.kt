package su.afk.yummy.tv.core.model.settings

/** Итог проверки индексатора раздач (JacRed или Jackett) из настроек. */
sealed interface TorrentIndexerCheckResult {
    /** Индексатор ответил и пригоден для поиска. */
    data object Ok : TorrentIndexerCheckResult

    /** Индексатор не отвечает: неверный адрес, сайт недоступен или заблокирован. */
    data object Unreachable : TorrentIndexerCheckResult

    /** Индексатор отклонил запрос: неверный или не указанный API-ключ (Jackett). */
    data object Unauthorized : TorrentIndexerCheckResult

    /** Индексатор ответил [httpCode] или телом, которых мы не ждём (например, адрес указывает не на него). */
    data class UnexpectedResponse(val httpCode: Int) : TorrentIndexerCheckResult
}
