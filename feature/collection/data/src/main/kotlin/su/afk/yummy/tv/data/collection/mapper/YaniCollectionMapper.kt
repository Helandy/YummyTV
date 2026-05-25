package su.afk.yummy.tv.data.collection.mapper

import su.afk.yummy.tv.data.collection.dto.YaniCollectionAnimeDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionPosterDto
import su.afk.yummy.tv.domain.collection.model.CollectionAnimeItem
import su.afk.yummy.tv.domain.collection.model.CollectionDetail

internal fun YaniCollectionDetailDto.toDomain(): CollectionDetail {
    val resolvedId = id ?: 0
    return CollectionDetail(
        id = resolvedId,
        title = title,
        description = description,
        views = views,
        posterUrl = posterPreviews.firstOrNull()?.toUrl(),
        animes = animes.mapNotNull { it.toDomain() },
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
