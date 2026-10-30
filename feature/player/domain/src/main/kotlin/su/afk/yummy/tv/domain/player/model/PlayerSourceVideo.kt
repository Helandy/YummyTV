package su.afk.yummy.tv.domain.player.model

data class PlayerSourceVideo(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val playerId: Int? = null,
    val iframeUrl: String,
    val views: Int? = null,
    val skips: PlayerSourceSkips = PlayerSourceSkips.Empty,
    /** Серверный прогресс просмотра серии (с других устройств); для подписей в списке серий. */
    val watchedEndTimeSeconds: Int? = null,
    val watchedDateSeconds: Long? = null,
    val durationSeconds: Int? = null,
)
