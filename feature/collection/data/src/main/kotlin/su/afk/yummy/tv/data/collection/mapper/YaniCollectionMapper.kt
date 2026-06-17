package su.afk.yummy.tv.data.collection.mapper

import su.afk.yummy.tv.data.collection.dto.YaniCollectionAnimeDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionPosterDto
import su.afk.yummy.tv.domain.collection.model.CollectionAnimeItem
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionSummary

internal fun YaniCollectionDetailDto.toDomain(fallbackId: Int): CollectionDetail {
    val resolvedId = id ?: fallbackId
    return CollectionDetail(
        id = resolvedId,
        title = title,
        description = description,
        views = views,
        posterUrl = posterPreviews.firstOrNull()?.toUrl(),
        animes = animes.mapNotNull { it.toDomain() },
    )
}

internal fun YaniCollectionDetailDto.toSummary(): CollectionSummary? {
    val resolvedId = id ?: return null
    val safeTitle = title.trim().takeIf { it.isNotEmpty() } ?: return null
    return CollectionSummary(
        id = resolvedId,
        title = safeTitle,
        description = description,
        posterUrl = posterPreviews.firstOrNull()?.toUrl(),
        likesCount = likes?.likes?.coerceAtLeast(0) ?: 0,
    )
}

private fun YaniCollectionAnimeDto.toDomain(): CollectionAnimeItem? {
    val resolvedId = animeId ?: return null
    return CollectionAnimeItem(
        id = resolvedId,
        title = title,
        posterUrl = poster?.toUrl(),
        rating = rating?.average,
    )
}

private fun YaniCollectionPosterDto.toUrl(): String? =
    (big ?: medium ?: fullsize ?: small)?.let { url ->
        when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") -> url.replaceFirst("http://", "https://")
            else -> url
        }
    }
