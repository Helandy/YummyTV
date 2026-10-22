package su.afk.yummy.tv.source.amedia.domain.model

/** Серия Amedia-плеера: [number] — номер на сайте, [vodId] — id записи в плеере `aser.pro/vod/{vodId}`. */
data class AmediaEpisode(
    val number: Int,
    val vodId: Long,
)
