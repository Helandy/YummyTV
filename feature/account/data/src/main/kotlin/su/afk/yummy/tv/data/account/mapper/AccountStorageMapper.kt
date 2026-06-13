package su.afk.yummy.tv.data.account.mapper

import su.afk.yummy.tv.core.storage.account.AccountAnimeListStateEntry
import su.afk.yummy.tv.core.storage.account.AccountCollectionItemEntry
import su.afk.yummy.tv.core.storage.account.AccountCollectionPageEntry
import su.afk.yummy.tv.core.storage.account.AccountCollectionsPageCache
import su.afk.yummy.tv.core.storage.account.AccountListStatEntry
import su.afk.yummy.tv.core.storage.account.AccountListStatsCache
import su.afk.yummy.tv.core.storage.account.AccountListStatsCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationAnimeEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationCountCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationCountEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationCountsCache
import su.afk.yummy.tv.core.storage.account.AccountNotificationEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationPageEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationsPageCache
import su.afk.yummy.tv.core.storage.account.AccountProfileEntry
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketEntry
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketsCache
import su.afk.yummy.tv.core.storage.account.AccountUserGenreStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserListCache
import su.afk.yummy.tv.core.storage.account.AccountUserListItemEntry
import su.afk.yummy.tv.core.storage.account.AccountUserListPageEntry
import su.afk.yummy.tv.core.storage.account.AccountUserListWatchStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserRatingEntry
import su.afk.yummy.tv.core.storage.account.AccountUserRatingStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserStatsCache
import su.afk.yummy.tv.core.storage.account.AccountUserStatsCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountUserTypeStatEntry
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionEntry
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionsCache
import su.afk.yummy.tv.domain.account.model.AnimeCollectionPoster
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingBucket
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserAnimePoster
import su.afk.yummy.tv.domain.account.model.UserAnimeTypeStat
import su.afk.yummy.tv.domain.account.model.UserGenreStat
import su.afk.yummy.tv.domain.account.model.UserListWatchStat
import su.afk.yummy.tv.domain.account.model.UserRatingStat
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.model.YaniAccount

internal fun YaniAccount.toProfileEntry(profileKey: String, cachedAt: Long): AccountProfileEntry =
    AccountProfileEntry(
        profileKey = profileKey,
        userId = id,
        nickname = nickname,
        avatarUrl = avatarUrl,
        cachedAt = cachedAt,
    )

internal fun AccountProfileEntry.toAccount(): YaniAccount =
    YaniAccount(
        id = userId,
        nickname = nickname,
        avatarUrl = avatarUrl,
    )

internal fun List<UserAnimeListItem>.toUserListCache(
    userId: Int,
    listId: Int,
    language: String,
    cachedAt: Long,
): AccountUserListCache =
    AccountUserListCache(
        entry = AccountUserListPageEntry(
            userId = userId,
            listId = listId,
            language = language,
            cachedAt = cachedAt,
        ),
        items = mapIndexed { index, item ->
            AccountUserListItemEntry(
                userId = userId,
                listId = listId,
                language = language,
                position = index,
                animeId = item.animeId,
                title = item.title,
                posterUrl = item.posterUrl,
                posterSmallUrl = item.poster?.small,
                posterMediumUrl = item.poster?.medium,
                posterBigUrl = item.poster?.big,
                posterFullsizeUrl = item.poster?.fullsize,
                posterMegaUrl = item.poster?.mega,
                rating = item.rating,
                year = item.year,
                userListId = item.list?.id,
                isFavorite = item.isFavorite,
            )
        },
    )

internal fun AccountUserListCache.toUserListItems(): List<UserAnimeListItem> =
    items.map { it.toUserListItem() }

internal fun AccountAnimeListStateEntry.toUserListItem(): UserAnimeListItem =
    UserAnimeListItem(
        animeId = animeId,
        title = "",
        posterUrl = null,
        poster = null,
        rating = null,
        year = null,
        list = listId.toUserAnimeList(),
        isFavorite = isFavorite,
    )

internal fun List<AnimeRatingBucket>.toRatingBucketsCache(
    animeId: Int,
    cachedAt: Long,
): AccountRatingBucketsCache =
    AccountRatingBucketsCache(
        entry = AccountRatingBucketCacheEntry(animeId = animeId, cachedAt = cachedAt),
        buckets = mapIndexed { index, bucket ->
            AccountRatingBucketEntry(
                animeId = animeId,
                position = index,
                rating = bucket.rating,
                count = bucket.count,
            )
        },
    )

internal fun AccountRatingBucketsCache.toRatingSummary(userRating: Int? = null): AnimeRatingSummary =
    AnimeRatingSummary(
        distribution = buckets.map { AnimeRatingBucket(rating = it.rating, count = it.count) },
        userRating = userRating,
    )

internal fun AccountUserRatingEntry.toUserRating(): Int? =
    rating?.takeIf { it in 1..10 }

internal fun AnimeListStats.toListStatsCache(animeId: Int, cachedAt: Long): AccountListStatsCache =
    AccountListStatsCache(
        entry = AccountListStatsCacheEntry(animeId = animeId, cachedAt = cachedAt),
        stats = counts.map { (listId, count) ->
            AccountListStatEntry(
                animeId = animeId,
                listId = listId,
                count = count,
            )
        },
    )

internal fun AccountListStatsCache.toAnimeListStats(): AnimeListStats =
    AnimeListStats(counts = stats.associate { it.listId to it.count })

internal fun List<AnimeCollectionSummary>.toCollectionsPageCache(
    pageKey: String,
    language: String,
    cachedAt: Long,
): AccountCollectionsPageCache =
    AccountCollectionsPageCache(
        entry = AccountCollectionPageEntry(
            pageKey = pageKey,
            language = language,
            cachedAt = cachedAt,
        ),
        items = mapIndexed { index, item ->
            AccountCollectionItemEntry(
                pageKey = pageKey,
                position = index,
                collectionId = item.id,
                title = item.title,
                description = item.description,
                posterUrl = item.posterUrl,
                posterSmallUrl = item.poster?.small,
                posterMediumUrl = item.poster?.medium,
                posterBigUrl = item.poster?.big,
                posterFullsizeUrl = item.poster?.fullsize,
                posterMegaUrl = item.poster?.mega,
                views = item.views,
            )
        },
    )

internal fun AccountCollectionsPageCache.toCollectionSummaries(): List<AnimeCollectionSummary> =
    items.map { it.toCollectionSummary() }

internal fun List<VideoSubscription>.toVideoSubscriptionsCache(
    userId: Int,
    language: String,
    cachedAt: Long,
): AccountVideoSubscriptionsCache =
    AccountVideoSubscriptionsCache(
        entry = AccountVideoSubscriptionCacheEntry(
            userId = userId,
            language = language,
            cachedAt = cachedAt,
        ),
        items = mapIndexed { index, item ->
            AccountVideoSubscriptionEntry(
                userId = userId,
                language = language,
                position = index,
                animeId = item.animeId,
                animeUrl = item.animeUrl,
                playerId = item.playerId,
                player = item.player,
                dubbing = item.dubbing,
                posterUrl = item.posterUrl,
                title = item.title,
            )
        },
    )

internal fun AccountVideoSubscriptionsCache.toVideoSubscriptions(): List<VideoSubscription> =
    items.map {
        VideoSubscription(
            animeId = it.animeId,
            animeUrl = it.animeUrl,
            playerId = it.playerId,
            player = it.player,
            dubbing = it.dubbing,
            posterUrl = it.posterUrl,
            title = it.title,
        )
    }

internal fun List<ProfileNotification>.toNotificationsPageCache(
    userId: Int,
    language: String,
    limit: Int,
    offset: Int,
    cachedAt: Long,
): AccountNotificationsPageCache =
    AccountNotificationsPageCache(
        entry = AccountNotificationPageEntry(
            userId = userId,
            language = language,
            limit = limit,
            offset = offset,
            cachedAt = cachedAt,
        ),
        items = mapIndexed { index, item ->
            AccountNotificationEntry(
                userId = userId,
                language = language,
                limit = limit,
                offset = offset,
                position = index,
                notificationId = item.id,
                dateSeconds = item.dateSeconds,
                title = item.title,
                text = item.text,
                clickUri = item.clickUri,
                type = item.type,
                subType = item.subType,
                viewed = item.viewed,
                objectId = item.objectId,
                animeSlug = item.animeSlug,
                isNewEpisode = item.isNewEpisode,
            )
        },
    )

internal fun AccountNotificationsPageCache.toNotifications(): List<ProfileNotification> =
    items.map {
        ProfileNotification(
            id = it.notificationId,
            dateSeconds = it.dateSeconds,
            title = it.title,
            text = it.text,
            clickUri = it.clickUri,
            type = it.type,
            subType = it.subType,
            viewed = it.viewed,
            objectId = it.objectId,
            animeSlug = it.animeSlug,
            isNewEpisode = it.isNewEpisode,
        )
    }

internal fun List<NotificationCount>.toNotificationCountsCache(
    userId: Int,
    cachedAt: Long,
): AccountNotificationCountsCache =
    AccountNotificationCountsCache(
        entry = AccountNotificationCountCacheEntry(userId = userId, cachedAt = cachedAt),
        items = mapIndexed { index, item ->
            AccountNotificationCountEntry(
                userId = userId,
                position = index,
                type = item.type,
                count = item.count,
            )
        },
    )

internal fun AccountNotificationCountsCache.toNotificationCounts(): List<NotificationCount> =
    items.map { NotificationCount(type = it.type, count = it.count) }

internal fun Int?.toNotificationAnimeEntry(
    slug: String,
    cachedAt: Long
): AccountNotificationAnimeEntry =
    AccountNotificationAnimeEntry(
        slug = slug,
        animeId = this,
        cachedAt = cachedAt,
    )

internal fun UserStats.toUserStatsCache(
    userId: Int,
    language: String,
    cachedAt: Long,
): AccountUserStatsCache =
    AccountUserStatsCache(
        entry = AccountUserStatsCacheEntry(
            userId = userId,
            language = language,
            cachedAt = cachedAt,
        ),
        genres = genres.mapIndexed { index, item ->
            AccountUserGenreStatEntry(
                userId = userId,
                language = language,
                position = index,
                genreId = item.id,
                title = item.title,
                count = item.count,
            )
        },
        ratings = ratings.mapIndexed { index, item ->
            AccountUserRatingStatEntry(
                userId = userId,
                language = language,
                position = index,
                rating = item.rating,
                count = item.count,
            )
        },
        lists = lists.mapIndexed { index, item ->
            AccountUserListWatchStatEntry(
                userId = userId,
                language = language,
                position = index,
                listId = item.id,
                title = item.title,
                href = item.href,
                seconds = item.seconds,
            )
        },
        types = types.mapIndexed { index, item ->
            AccountUserTypeStatEntry(
                userId = userId,
                language = language,
                position = index,
                typeId = item.id,
                title = item.title,
                shortName = item.shortName,
                count = item.count,
            )
        },
    )

internal fun AccountUserStatsCache.toUserStats(): UserStats =
    UserStats(
        genres = genres.map { UserGenreStat(id = it.genreId, title = it.title, count = it.count) },
        ratings = ratings.map { UserRatingStat(rating = it.rating, count = it.count) },
        lists = lists.map {
            UserListWatchStat(
                id = it.listId,
                title = it.title,
                href = it.href,
                seconds = it.seconds,
            )
        },
        types = types.map {
            UserAnimeTypeStat(
                id = it.typeId,
                title = it.title,
                shortName = it.shortName,
                count = it.count,
            )
        },
    )

private fun AccountUserListItemEntry.toUserListItem(): UserAnimeListItem {
    val poster = userAnimePosterOrNull()
    return UserAnimeListItem(
        animeId = animeId,
        title = title,
        posterUrl = posterUrl ?: poster?.standardUrl,
        poster = poster,
        rating = rating,
        year = year,
        list = userListId.toUserAnimeList(),
        isFavorite = isFavorite,
    )
}

private fun AccountCollectionItemEntry.toCollectionSummary(): AnimeCollectionSummary {
    val poster = collectionPosterOrNull()
    return AnimeCollectionSummary(
        id = collectionId,
        title = title,
        description = description,
        posterUrl = posterUrl ?: poster?.standardUrl,
        poster = poster,
        views = views,
    )
}

private fun AccountUserListItemEntry.userAnimePosterOrNull(): UserAnimePoster? {
    if (posterSmallUrl == null &&
        posterMediumUrl == null &&
        posterBigUrl == null &&
        posterFullsizeUrl == null &&
        posterMegaUrl == null
    ) {
        return null
    }
    return UserAnimePoster(
        small = posterSmallUrl,
        medium = posterMediumUrl,
        big = posterBigUrl,
        fullsize = posterFullsizeUrl,
        mega = posterMegaUrl,
    )
}

private fun AccountCollectionItemEntry.collectionPosterOrNull(): AnimeCollectionPoster? {
    if (posterSmallUrl == null &&
        posterMediumUrl == null &&
        posterBigUrl == null &&
        posterFullsizeUrl == null &&
        posterMegaUrl == null
    ) {
        return null
    }
    return AnimeCollectionPoster(
        small = posterSmallUrl,
        medium = posterMediumUrl,
        big = posterBigUrl,
        fullsize = posterFullsizeUrl,
        mega = posterMegaUrl,
    )
}

private val UserAnimePoster.standardUrl: String?
    get() = big ?: medium ?: fullsize ?: small

private val AnimeCollectionPoster.standardUrl: String?
    get() = big ?: medium ?: fullsize ?: small
