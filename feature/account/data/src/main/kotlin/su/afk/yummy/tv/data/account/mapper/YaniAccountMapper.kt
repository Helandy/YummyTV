package su.afk.yummy.tv.data.account.mapper

import su.afk.yummy.tv.data.account.dto.YaniAccountPosterDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionSummaryDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationCountDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationDto
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.dto.YaniUserAnimeDto
import su.afk.yummy.tv.data.account.dto.YaniUserAnimeTypeStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserGenreStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserListWatchStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserRatingStatDto
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserAnimeTypeStat
import su.afk.yummy.tv.domain.account.model.UserGenreStat
import su.afk.yummy.tv.domain.account.model.UserListWatchStat
import su.afk.yummy.tv.domain.account.model.UserRatingStat
import su.afk.yummy.tv.domain.account.model.YaniAccount

internal fun YaniProfileDto.toAccount(): YaniAccount =
    YaniAccount(
        id = id,
        nickname = nickname,
        avatarUrl = avatars?.full?.toHttpsUrl() ?: avatars?.big?.toHttpsUrl() ?: avatars?.small?.toHttpsUrl(),
    )

internal fun YaniUserAnimeDto.toUserListItem(): UserAnimeListItem? {
    val id = animeId ?: return null
    return UserAnimeListItem(
        animeId = id,
        title = title,
        posterUrl = poster?.bestUrl(),
        rating = rating?.takeIf { it > 0.0 },
        year = year?.takeIf { it > 0 },
        list = user?.list?.list?.id.toUserAnimeList(),
        isFavorite = user?.list?.isFav == true,
    )
}

internal fun YaniCollectionSummaryDto.toCollectionSummary(): AnimeCollectionSummary? {
    val id = id ?: return null
    return AnimeCollectionSummary(
        id = id,
        title = title,
        description = description,
        posterUrl = animes.firstOrNull()?.poster?.bestUrl(),
        views = views,
    )
}

internal fun Int?.toUserAnimeList(): UserAnimeList? =
    UserAnimeList.entries.firstOrNull { it.id == this }

internal fun YaniAccountPosterDto.bestUrl(): String? =
    mega?.toHttpsUrl() ?: huge?.toHttpsUrl() ?: big?.toHttpsUrl() ?: medium?.toHttpsUrl() ?: fullsize?.toHttpsUrl() ?: small?.toHttpsUrl()

internal fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}

internal fun YaniUserGenreStatDto.toGenreStat(): UserGenreStat =
    UserGenreStat(id = id, title = title, count = count)

internal fun YaniUserRatingStatDto.toRatingStat(): UserRatingStat =
    UserRatingStat(rating = rating, count = count)

internal fun YaniUserListWatchStatDto.toListWatchStat(): UserListWatchStat? {
    val info = list ?: return null
    return UserListWatchStat(
        id = info.id ?: return null,
        title = info.title,
        href = info.href,
        seconds = seconds,
    )
}

internal fun YaniUserAnimeTypeStatDto.toAnimeTypeStat(): UserAnimeTypeStat? {
    val info = type ?: return null
    return UserAnimeTypeStat(
        id = info.value,
        title = info.name,
        shortName = info.shortname,
        count = count,
    )
}

internal fun YaniNotificationDto.toNotification(): ProfileNotification =
    ProfileNotification(
        id = id,
        dateSeconds = date,
        title = titleHtml.toPlainText(),
        text = textHtml.toPlainText(),
        clickUri = clickUri,
        type = type,
        subType = subType,
        viewed = viewed,
        objectId = objectId,
    )

internal fun YaniNotificationCountDto.toNotificationCount(): NotificationCount =
    NotificationCount(type = type, count = count)

private fun String.toPlainText(): String =
    replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
