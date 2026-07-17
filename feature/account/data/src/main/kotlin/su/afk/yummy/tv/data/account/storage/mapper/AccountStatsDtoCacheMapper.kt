package su.afk.yummy.tv.data.account.storage.mapper

import su.afk.yummy.tv.core.storage.account.AccountUserGenreStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserListWatchStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserProfileSummaryCache
import su.afk.yummy.tv.core.storage.account.AccountUserProfileSummaryCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountUserProfileWatchHistoryEntry
import su.afk.yummy.tv.core.storage.account.AccountUserProfileWatchTypeEntry
import su.afk.yummy.tv.core.storage.account.AccountUserRatingStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserStatsCache
import su.afk.yummy.tv.core.storage.account.AccountUserStatsCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountUserTypeStatEntry
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.account.dto.YaniUserAnimeTypeStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserGenreStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserListWatchStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserProfileDto
import su.afk.yummy.tv.data.account.dto.YaniUserRatingStatDto

internal data class YaniUserStatsDtoBundle(
    val genres: List<YaniUserGenreStatDto>,
    val ratings: List<YaniUserRatingStatDto>,
    val lists: List<YaniUserListWatchStatDto>,
    val types: List<YaniUserAnimeTypeStatDto>,
)

internal fun YaniUserStatsDtoBundle.toUserStatsCache(
    userId: Int,
    language: String,
    cachedAt: Long,
): AccountUserStatsCache =
    AccountUserStatsCache(
        entry = AccountUserStatsCacheEntry(userId, language, cachedAt),
        genres = genres.mapIndexed { index, item ->
            AccountUserGenreStatEntry(userId, language, index, item.id, item.title, item.count)
        },
        ratings = ratings.mapIndexed { index, item ->
            AccountUserRatingStatEntry(userId, language, index, item.rating, item.count)
        },
        lists = lists.mapNotNull { item ->
            val list = item.list ?: return@mapNotNull null
            val id = list.id ?: return@mapNotNull null
            Triple(id, list, item)
        }.mapIndexed { index, (id, list, item) ->
            AccountUserListWatchStatEntry(
                userId, language, index, id, list.title, list.href, item.seconds,
            )
        },
        types = types.mapNotNull { item ->
            item.type?.let { it to item }
        }.mapIndexed { index, (type, item) ->
            AccountUserTypeStatEntry(
                userId, language, index, type.value, type.name, type.shortname, item.count,
            )
        },
    )

internal fun YaniUserProfileDto.toUserProfileSummaryCache(
    userId: Int,
    language: String,
    cachedAt: Long,
): AccountUserProfileSummaryCache {
    val counts = counts.orEmpty()
    return AccountUserProfileSummaryCache(
        entry = AccountUserProfileSummaryCacheEntry(
            userId = userId,
            language = language,
            cachedAt = cachedAt,
            nickname = nickname,
            avatarUrl = avatars?.full?.toHttpsUrl() ?: avatars?.big?.toHttpsUrl()
            ?: avatars?.small?.toHttpsUrl(),
            bannerUrl = banner?.cropped?.toHttpsUrl() ?: banner?.full?.toHttpsUrl(),
            registerDateSeconds = registerDate ?: 0L,
            birthDateSeconds = birthDate ?: 0L,
            sex = when (sex) {
                1 -> 1
                2 -> 2
                else -> 0
            },
            about = about.orEmpty().trim(),
            daysOnline = daysOnline ?: 0,
            watchingCount = counts["0"] ?: 0,
            plannedCount = counts["1"] ?: 0,
            completedCount = counts["2"] ?: 0,
            droppedCount = counts["3"] ?: 0,
            postponedCount = counts["5"] ?: 0,
            favoriteCount = counts["4"] ?: 0,
            friendsCount = friends?.friends ?: 0,
            reviewsCount = reviewsCount?.takeIf { it > 0 } ?: reviewsCountObject?.approved ?: 0,
            commentsCount = commentsCount ?: 0,
            postsCount = postsCount?.approved ?: 0,
            collectionsCount = collectionsCount ?: 0,
        ),
        watchTypes = watches?.sum.orEmpty()
            .filter { it.spentTime.coerceAtLeast(0L) > 0L }
            .mapIndexed { index, item ->
                AccountUserProfileWatchTypeEntry(
                    userId, language, index, item.value, item.alias, item.name, item.shortname,
                    item.spentTime.coerceAtLeast(0L),
                )
            },
        watchHistory = watches?.history.orEmpty().mapIndexed { index, item ->
            AccountUserProfileWatchHistoryEntry(
                userId, language, index, item.dateSeconds, item.duration.coerceAtLeast(0L),
                item.episodeCount.coerceAtLeast(0),
            )
        },
    )
}
