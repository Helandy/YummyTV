package su.afk.yummy.tv.domain.home.model

data class HomeFeedItem(
    val id: Int,
    val title: String,
    val description: String,
    val poster: HomePoster?,
    val rating: Double?,
    val year: Int?,
    val action: HomeFeedItemAction,
    /** Номер последней вышедшей серии; заполнен только в секции новых серий. */
    val episodeNumber: Int? = null,
    /**
     * Когда вышла эта серия, epoch-секунды; заполнено вместе с [episodeNumber]. Ровная полночь UTC
     * означает, что у источника известен только день, без времени.
     */
    val airedAtSeconds: Long? = null,
    /** Серия [episodeNumber] уже отмечена просмотренной локально. */
    val isWatched: Boolean = false,
)
