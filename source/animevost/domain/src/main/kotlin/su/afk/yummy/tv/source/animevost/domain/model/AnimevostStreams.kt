package su.afk.yummy.tv.source.animevost.domain.model

/** Прямые mp4 серии: метка качества `"720p"` → адрес, по возрастанию качества; не пусто. */
data class AnimevostStreams(
    val qualities: Map<String, String>,
)
