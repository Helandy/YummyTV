package su.afk.yummy.tv.source.ailiberty.domain.model

/** Потоки серии: качество («480p») → адрес HLS-плейлиста, по возрастанию качества; лучшее — последнее. */
data class AiLibertyStreams(
    val qualities: Map<String, String>,
)
