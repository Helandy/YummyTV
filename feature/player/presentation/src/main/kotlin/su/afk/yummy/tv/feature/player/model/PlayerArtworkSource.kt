package su.afk.yummy.tv.feature.player.model

/** Откуда брать обложку медиа-уведомления: скриншот серии, iframe (превью Kodik) или постер. */
internal data class PlayerArtworkSource(
    val screenshotUrl: String,
    val episodeUrl: String,
    val posterUrl: String,
)
