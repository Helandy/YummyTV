package su.afk.yummy.tv.data.collection.storage.mapper

import su.afk.yummy.tv.core.storage.collection.CollectionAnimeItemEntry
import su.afk.yummy.tv.core.storage.collection.CollectionDetailCache
import su.afk.yummy.tv.core.storage.collection.CollectionDetailEntry
import su.afk.yummy.tv.domain.collection.model.CollectionAnimeItem
import su.afk.yummy.tv.domain.collection.model.CollectionDetail

internal fun CollectionDetail.toCollectionDetailCache(
    language: String,
    cachedAt: Long,
): CollectionDetailCache =
    CollectionDetailCache(
        entry = CollectionDetailEntry(
            collectionId = id,
            language = language,
            title = title,
            description = description,
            views = views,
            posterUrl = posterUrl,
            cachedAt = cachedAt,
        ),
        items = animes.mapIndexed { index, item ->
            CollectionAnimeItemEntry(
                collectionId = id,
                language = language,
                position = index,
                animeId = item.id,
                title = item.title,
                posterUrl = item.posterUrl,
                rating = item.rating,
            )
        },
    )

internal fun CollectionDetailCache.toCollectionDetail(): CollectionDetail =
    CollectionDetail(
        id = entry.collectionId,
        title = entry.title,
        description = entry.description,
        views = entry.views,
        posterUrl = entry.posterUrl,
        animes = items
            .sortedBy { it.position }
            .map {
                CollectionAnimeItem(
                    id = it.animeId,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    rating = it.rating,
                )
            },
    )
