package su.afk.yummy.tv.domain.player.model

data class PlayerSourceEpisode(
    val id: Int = 0,
    val playerId: Int? = null,
    val number: String = "",
    val iframeUrl: String = "",
    val screenshotUrl: String = "",
    val skips: PlayerSourceSkips = PlayerSourceSkips.Empty,
    /** Серверный прогресс просмотра серии (с других устройств); для подписей в списке серий. */
    val watchedEndTimeSeconds: Int? = null,
    val watchedDateSeconds: Long? = null,
    val durationSeconds: Int? = null,
)
