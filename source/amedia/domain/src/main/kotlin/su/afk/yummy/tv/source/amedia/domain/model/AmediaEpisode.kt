package su.afk.yummy.tv.source.amedia.domain.model

/** Серия Amedia-плеера: [number] — номер на сайте, [source] — откуда она играет. */
data class AmediaEpisode(
    val number: Int,
    val source: AmediaEpisodeSource,
)
