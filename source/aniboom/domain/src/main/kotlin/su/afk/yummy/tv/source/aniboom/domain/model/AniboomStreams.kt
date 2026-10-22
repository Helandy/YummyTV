package su.afk.yummy.tv.source.aniboom.domain.model

/**
 * Поток серии: мастер-плейлист HLS ([hlsUrl]) и, если в нём больше одного качества, готовые ссылки по меткам
 * (`"720p"`, по возрастанию). Каждая ссылка ведёт на плейлист вместе со звуком.
 */
data class AniboomStreams(
    val hlsUrl: String,
    val qualities: Map<String, String> = emptyMap(),
)
