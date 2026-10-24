package su.afk.yummy.tv.domain.home.model

import su.afk.yummy.tv.core.model.anime.AnimePoster

data class HomeFeedItem(
    val id: Int,
    val title: String,
    val description: String,
    val poster: AnimePoster?,
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
    /** Заполнено только у коллекций. */
    val collection: HomeCollectionPreview? = null,
)
