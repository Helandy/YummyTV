package su.afk.yummy.tv.source.anidub.domain.model

/**
 * Потоки серии: [masterUrl] — HLS-мастер (адаптивное «Авто»), [qualities] — отдельные варианты «720p» → адрес по возрастанию
 * качества (может быть пусто, если мастер не разобрался). [headers] обязательны плееру: ссылки на CDN подписаны User-Agent, с
 * которым запрошен мастер, и с любым другим UA отвечают 403.
 */
data class AniDubStreams(
    val qualities: Map<String, String>,
    val masterUrl: String,
    val headers: Map<String, String> = emptyMap(),
)
