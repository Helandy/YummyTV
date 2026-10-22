package su.afk.yummy.tv.core.model.settings

enum class DetailsButtonAction {
    WATCH,
    LIBRARY,
    FAVORITE,
    EPISODES,
    SUBSCRIPTIONS,
    FULL_DETAILS,
    TRAILERS,
    SIMILAR,
    VIEWING_ORDER,
    RATING,
    COLLECTIONS,
    COMMENTS,
    REVIEWS,
    BLOGGER_VIDEOS,
    SCREENSHOTS,
    ;

    companion object {
        /** Порядок кнопок на экране деталей. */
        val defaultOrder: List<DetailsButtonAction> = listOf(
            WATCH,
            LIBRARY,
            FAVORITE,
            EPISODES,
            FULL_DETAILS,
            SUBSCRIPTIONS,
            TRAILERS,
            SIMILAR,
            VIEWING_ORDER,
            RATING,
            COLLECTIONS,
            COMMENTS,
            REVIEWS,
            BLOGGER_VIDEOS,
            SCREENSHOTS,
        )
    }
}
