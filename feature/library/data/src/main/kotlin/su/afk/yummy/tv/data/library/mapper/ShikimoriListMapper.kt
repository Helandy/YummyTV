package su.afk.yummy.tv.data.library.mapper

import su.afk.yummy.tv.data.library.dto.ShikimoriListEntryDto
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.utils.normalizedExternalScore
import su.afk.yummy.tv.domain.library.utils.toMalSeriesType
import su.afk.yummy.tv.domain.library.utils.toShikimoriStatus
import su.afk.yummy.tv.domain.library.utils.toUserAnimeListFromShikimori

/** `true`, если запись JSON Shikimori относится к манге/ранобэ, а не к аниме. */
internal fun ShikimoriListEntryDto.isNonAnimeTarget(): Boolean {
    val type = targetType?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    return !type.equals(ShikimoriListEntryDto.TARGET_TYPE_ANIME, ignoreCase = true)
}

internal fun ShikimoriListEntryDto.toExternalListEntry(): ExternalListEntry? {
    if (isNonAnimeTarget()) return null
    val resolvedId = (targetId ?: anime?.id)?.takeIf { it > 0 } ?: return null
    val resolvedList = status.toUserAnimeListFromShikimori() ?: return null
    val resolvedTitle = listOfNotNull(
        targetTitleRu,
        targetTitle,
        anime?.russian,
        anime?.name,
    ).firstOrNull { it.isNotBlank() }.orEmpty().trim()
    return ExternalListEntry(
        externalId = resolvedId,
        title = resolvedTitle,
        list = resolvedList,
        score = score.normalizedExternalScore(),
        watchedEpisodes = episodes.coerceAtLeast(0),
        rewatches = rewatches.coerceAtLeast(0),
        seriesType = anime?.kind?.toMalSeriesType(),
        totalEpisodes = (anime?.episodes ?: 0).coerceAtLeast(0),
    )
}

internal fun ExternalListEntry.toShikimoriDto(): ShikimoriListEntryDto =
    ShikimoriListEntryDto(
        targetTitle = title,
        targetId = externalId,
        targetType = ShikimoriListEntryDto.TARGET_TYPE_ANIME,
        score = score.normalizedExternalScore() ?: 0,
        status = list.toShikimoriStatus(),
        rewatches = rewatches.coerceAtLeast(0),
        episodes = watchedEpisodes.coerceAtLeast(0),
        text = null,
    )
