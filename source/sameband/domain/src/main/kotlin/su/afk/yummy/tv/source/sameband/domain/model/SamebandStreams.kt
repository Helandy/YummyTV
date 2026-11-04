package su.afk.yummy.tv.source.sameband.domain.model

/**
 * Потоки серии: [qualities] — «720p» → адрес HLS по возрастанию качества (последнее — лучшее), [headers] обязательны плееру
 * (сайт отдаёт HLS только с Referer страницы).
 */
data class SamebandStreams(
    val qualities: Map<String, String>,
    val headers: Map<String, String> = emptyMap(),
)
