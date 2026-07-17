package su.afk.yummy.tv.data.account.storage.mapper

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import su.afk.yummy.tv.core.storage.account.ACCOUNT_USER_PROFILE_CONTENT_FRIENDS
import su.afk.yummy.tv.core.storage.account.ACCOUNT_USER_PROFILE_CONTENT_POSTS
import su.afk.yummy.tv.core.storage.account.ACCOUNT_USER_PROFILE_CONTENT_REVIEWS
import su.afk.yummy.tv.core.storage.account.AccountCollectionItemEntry
import su.afk.yummy.tv.core.storage.account.AccountCollectionPageEntry
import su.afk.yummy.tv.core.storage.account.AccountCollectionsPageCache
import su.afk.yummy.tv.core.storage.account.AccountNotificationEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationPageEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationsPageCache
import su.afk.yummy.tv.core.storage.account.AccountProfileEntry
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketEntry
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketsCache
import su.afk.yummy.tv.core.storage.account.AccountUserFriendEntry
import su.afk.yummy.tv.core.storage.account.AccountUserFriendsPageCache
import su.afk.yummy.tv.core.storage.account.AccountUserListCache
import su.afk.yummy.tv.core.storage.account.AccountUserListItemEntry
import su.afk.yummy.tv.core.storage.account.AccountUserListPageEntry
import su.afk.yummy.tv.core.storage.account.AccountUserPostEntry
import su.afk.yummy.tv.core.storage.account.AccountUserPostsPageCache
import su.afk.yummy.tv.core.storage.account.AccountUserProfileContentPageEntry
import su.afk.yummy.tv.core.storage.account.AccountUserReviewEntry
import su.afk.yummy.tv.core.storage.account.AccountUserReviewsPageCache
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionEntry
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionsCache
import su.afk.yummy.tv.core.utils.htmlToPlainText
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.account.dto.YaniAccountPosterDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionSummaryDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationDto
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.dto.YaniRatingBucketDto
import su.afk.yummy.tv.data.account.dto.YaniUserAnimeDto
import su.afk.yummy.tv.data.account.dto.YaniUserFriendDto
import su.afk.yummy.tv.data.account.dto.YaniUserPostDto
import su.afk.yummy.tv.data.account.dto.YaniUserReviewDto
import su.afk.yummy.tv.data.account.dto.YaniVideoSubscriptionDto

internal fun YaniProfileDto.toProfileEntry(
    profileKey: String,
    cachedAt: Long
): AccountProfileEntry =
    AccountProfileEntry(
        profileKey = profileKey,
        userId = id,
        nickname = nickname,
        avatarUrl = avatars?.full?.toHttpsUrl() ?: avatars?.big?.toHttpsUrl()
        ?: avatars?.small?.toHttpsUrl(),
        cachedAt = cachedAt,
    )

internal fun List<YaniUserAnimeDto>.toUserListCache(
    userId: Int,
    listId: Int,
    language: String,
    cachedAt: Long,
): AccountUserListCache =
    AccountUserListCache(
        entry = AccountUserListPageEntry(userId, listId, language, cachedAt),
        items = mapNotNull { item ->
            val animeId = item.animeId ?: return@mapNotNull null
            animeId to item
        }.mapIndexed { index, (animeId, item) ->
            val poster = item.poster
            AccountUserListItemEntry(
                userId = userId,
                listId = listId,
                language = language,
                position = index,
                animeId = animeId,
                title = item.title,
                posterUrl = poster?.standardUrl(),
                posterSmallUrl = poster?.small?.toHttpsUrl(),
                posterMediumUrl = poster?.medium?.toHttpsUrl(),
                posterBigUrl = poster?.big?.toHttpsUrl(),
                posterFullsizeUrl = poster?.fullsize?.toHttpsUrl(),
                posterMegaUrl = poster?.mega?.toHttpsUrl(),
                rating = item.rating?.takeIf { it > 0.0 },
                userRating = item.user?.rating?.toInt()?.takeIf { it in 1..10 },
                year = item.year?.takeIf { it > 0 },
                userListId = item.user?.list?.list?.id,
                isFavorite = item.user?.list?.isFav == true,
                updatedAtSeconds = item.date?.takeIf { it > 0L },
            )
        },
    )

internal fun List<YaniRatingBucketDto>.toRatingBucketsCache(
    animeId: Int,
    cachedAt: Long,
): AccountRatingBucketsCache =
    AccountRatingBucketsCache(
        entry = AccountRatingBucketCacheEntry(animeId, cachedAt),
        buckets = mapIndexed { index, item ->
            AccountRatingBucketEntry(animeId, index, item.rating, item.count)
        },
    )

internal fun List<YaniCollectionSummaryDto>.toCollectionsPageCache(
    pageKey: String,
    language: String,
    cachedAt: Long,
): AccountCollectionsPageCache =
    AccountCollectionsPageCache(
        entry = AccountCollectionPageEntry(pageKey, language, cachedAt),
        items = mapNotNull { item ->
            val id = item.id ?: return@mapNotNull null
            id to item
        }.mapIndexed { index, (id, item) ->
            val poster = item.posterPreviews.firstOrNull() ?: item.animes.firstOrNull()?.poster
            AccountCollectionItemEntry(
                pageKey = pageKey,
                position = index,
                collectionId = id,
                title = item.title,
                description = item.description,
                posterUrl = poster?.standardUrl(),
                posterSmallUrl = poster?.small?.toHttpsUrl(),
                posterMediumUrl = poster?.medium?.toHttpsUrl(),
                posterBigUrl = poster?.big?.toHttpsUrl(),
                posterFullsizeUrl = poster?.fullsize?.toHttpsUrl(),
                posterMegaUrl = poster?.mega?.toHttpsUrl(),
                views = item.views,
            )
        },
    )

internal fun List<YaniVideoSubscriptionDto>.toVideoSubscriptionsCache(
    userId: Int,
    language: String,
    cachedAt: Long,
): AccountVideoSubscriptionsCache =
    AccountVideoSubscriptionsCache(
        entry = AccountVideoSubscriptionCacheEntry(userId, language, cachedAt),
        items = mapNotNull { item ->
            val animeId = item.animeId.toFlexibleInt() ?: return@mapNotNull null
            val sub = item.sub ?: return@mapNotNull null
            Triple(animeId, sub, item)
        }.mapIndexed { index, (animeId, sub, item) ->
            AccountVideoSubscriptionEntry(
                userId = userId,
                language = language,
                position = index,
                animeId = animeId,
                animeUrl = item.animeUrl,
                playerId = sub.playerId.toFlexibleInt(),
                player = sub.player,
                dubbing = sub.dubbing,
                posterUrl = item.poster?.bestUrl(),
                title = item.title,
            )
        },
    )

internal fun List<YaniNotificationDto>.toNotificationsPageCache(
    userId: Int,
    language: String,
    limit: Int,
    offset: Int,
    cachedAt: Long,
): AccountNotificationsPageCache =
    AccountNotificationsPageCache(
        entry = AccountNotificationPageEntry(userId, language, limit, offset, cachedAt),
        items = mapIndexed { index, item ->
            AccountNotificationEntry(
                userId = userId,
                language = language,
                limit = limit,
                offset = offset,
                position = index,
                notificationId = item.id,
                dateSeconds = item.date,
                title = item.titleHtml.htmlToPlainText(),
                text = item.textHtml.htmlToPlainText(),
                clickUri = item.clickUri,
                type = item.type,
                subType = item.subType,
                viewed = item.viewed,
                objectId = item.objectId,
                animeSlug = item.clickUri.toCatalogItemSlug(),
                isNewEpisode = item.type == "anime_episode" && item.subType == "new_episode",
            )
        },
    )

internal fun List<YaniUserFriendDto>.toUserFriendsPageCache(
    userId: Int,
    language: String,
    limit: Int,
    offset: Int,
    cachedAt: Long,
): AccountUserFriendsPageCache =
    AccountUserFriendsPageCache(
        entry = AccountUserProfileContentPageEntry(
            userId, language, ACCOUNT_USER_PROFILE_CONTENT_FRIENDS, limit, offset, cachedAt,
        ),
        items = filter { it.id > 0 }.mapIndexed { index, item ->
            AccountUserFriendEntry(
                userId, language, limit, offset, index, item.id, item.nickname,
                item.avatars?.full?.toHttpsUrl() ?: item.avatars?.big?.toHttpsUrl()
                ?: item.avatars?.small?.toHttpsUrl(),
                item.lastOnline, item.friendStatus.ifBlank { item.list },
            )
        },
    )

internal fun List<YaniUserReviewDto>.toUserReviewsPageCache(
    userId: Int,
    language: String,
    limit: Int,
    offset: Int,
    cachedAt: Long,
): AccountUserReviewsPageCache =
    AccountUserReviewsPageCache(
        entry = AccountUserProfileContentPageEntry(
            userId, language, ACCOUNT_USER_PROFILE_CONTENT_REVIEWS, limit, offset, cachedAt,
        ),
        items = filter { it.reviewId > 0 }.mapIndexed { index, item ->
            AccountUserReviewEntry(
                userId = userId,
                language = language,
                limit = limit,
                offset = offset,
                position = index,
                reviewId = item.reviewId,
                animeId = item.anime?.animeId?.takeIf { it > 0 }
                    ?: item.animeId.takeIf { it > 0 } ?: 0,
                animeTitle = item.anime?.title.orEmpty(),
                animePosterUrl = item.anime?.poster?.bestUrl(),
                textPreview = item.textPreview,
                rating = item.rating?.average,
                likes = item.likes?.likes ?: 0,
                dislikes = item.likes?.dislikes ?: 0,
                commentsCount = item.commentsCount,
                updatedAtSeconds = item.updatedAt,
            )
        },
    )

internal fun List<YaniUserPostDto>.toUserPostsPageCache(
    userId: Int,
    language: String,
    limit: Int,
    offset: Int,
    cachedAt: Long,
): AccountUserPostsPageCache =
    AccountUserPostsPageCache(
        entry = AccountUserProfileContentPageEntry(
            userId, language, ACCOUNT_USER_PROFILE_CONTENT_POSTS, limit, offset, cachedAt,
        ),
        items = filter { it.id > 0 }.mapIndexed { index, item ->
            AccountUserPostEntry(
                userId,
                language,
                limit,
                offset,
                index,
                item.id,
                item.title,
                item.previewImage?.toHttpsUrl(),
                item.contentPreview,
                item.category?.title.orEmpty().ifBlank { item.category?.uri.orEmpty() },
                item.createdAt,
            )
        },
    )

private fun YaniAccountPosterDto.standardUrl(): String? =
    big?.toHttpsUrl() ?: medium?.toHttpsUrl() ?: fullsize?.toHttpsUrl() ?: small?.toHttpsUrl()

private fun YaniAccountPosterDto.bestUrl(): String? =
    mega?.toHttpsUrl() ?: huge?.toHttpsUrl() ?: big?.toHttpsUrl() ?: medium?.toHttpsUrl()
    ?: fullsize?.toHttpsUrl() ?: small?.toHttpsUrl()

private fun JsonElement?.toFlexibleInt(): Int? {
    val primitive = this as? JsonPrimitive ?: return null
    primitive.intOrNull?.let { return it }
    primitive.doubleOrNull?.let { return it.toInt() }
    return primitive.content.toIntOrNull()
}

private fun String.toCatalogItemSlug(): String? =
    substringAfter("/catalog/item/", missingDelimiterValue = "")
        .substringBefore("?")
        .substringBefore("#")
        .substringBefore("/")
        .takeIf { it.isNotBlank() }
