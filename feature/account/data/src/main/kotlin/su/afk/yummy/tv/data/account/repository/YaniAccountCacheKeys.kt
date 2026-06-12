package su.afk.yummy.tv.data.account.repository

import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.settings.withYaniContentLanguage

internal object YaniAccountCacheKeys {
    const val PRIVATE_USER_PREFIX = "account_user_"

    fun profileCurrent() = "account_profile_current"
    fun profileUser(userId: Int) = "account_user_${userId}_profile"
    fun userPrefix(userId: Int) = "account_user_${userId}_"
    fun userList(userId: Int, listId: Int) = "account_user_${userId}_list_$listId"
    fun userList(userId: Int, listId: Int, language: YaniContentLanguage) =
        userList(userId, listId).withYaniContentLanguage(language)

    fun animeListState(userId: Int, animeId: Int) =
        "account_user_${userId}_anime_${animeId}_list_state"

    fun ratingBuckets(animeId: Int) = "account_anime_${animeId}_rating_buckets"
    fun userRating(userId: Int, animeId: Int) = "account_user_${userId}_anime_${animeId}_rating"
    fun listStats(animeId: Int) = "account_anime_${animeId}_list_stats"
    fun animeCollections(animeId: Int, limit: Int, offset: Int) =
        "account_anime_${animeId}_collections_${limit}_$offset"
    fun animeCollections(animeId: Int, limit: Int, offset: Int, language: YaniContentLanguage) =
        animeCollections(animeId, limit, offset).withYaniContentLanguage(language)

    fun collections(limit: Int, offset: Int) = "account_collections_${limit}_$offset"
    fun collections(limit: Int, offset: Int, language: YaniContentLanguage) =
        collections(limit, offset).withYaniContentLanguage(language)

    fun subscriptions(userId: Int) = "account_user_${userId}_video_subscriptions"
    fun subscriptions(userId: Int, language: YaniContentLanguage) =
        subscriptions(userId).withYaniContentLanguage(language)

    fun userStats(userId: Int) = "account_user_${userId}_stats"
    fun userStats(userId: Int, language: YaniContentLanguage) =
        userStats(userId).withYaniContentLanguage(language)

    fun notifications(userId: Int, limit: Int, offset: Int) =
        "account_user_${userId}_notifications_${limit}_$offset"
    fun notifications(userId: Int, limit: Int, offset: Int, language: YaniContentLanguage) =
        notifications(userId, limit, offset).withYaniContentLanguage(language)

    fun notificationCounts(userId: Int) = "account_user_${userId}_notification_counts"
    fun notificationAnime(slug: String) = "account_notification_anime_$slug"
}

internal const val ACCOUNT_SHORT_TTL_MS = 5 * 60 * 1000L
internal const val ACCOUNT_MEDIUM_TTL_MS = 30 * 60 * 1000L
internal const val ACCOUNT_LONG_TTL_MS = 6 * 60 * 60 * 1000L
internal const val ANIME_LIST_STATE_TTL_MS = 24 * 60 * 60 * 1000L
