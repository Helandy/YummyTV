package su.afk.yummy.tv.source.studiomir.domain.model

/**
 * Потоки серии: [qualities] — варианты «720p» → адрес HLS по возрастанию качества, [masterUrl] — адрес лучшего из них (по
 * умолчанию). [headers] — заголовки запросов плеера (у сайта обязательных нет, поле оставлено для единообразия источников).
 */
data class StudioMirStreams(
    val qualities: Map<String, String>,
    val masterUrl: String,
    val headers: Map<String, String> = emptyMap(),
)
