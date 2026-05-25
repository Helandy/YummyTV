package su.afk.yummy.tv.data.account.mapper

import su.afk.yummy.tv.data.account.dto.YaniAccountPosterDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionSummaryDto
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.dto.YaniUserAnimeDto
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
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
