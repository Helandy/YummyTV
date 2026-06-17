package su.afk.yummy.tv.data.collection.storage.mapper

import su.afk.yummy.tv.core.storage.collection.CollectionAnimeItemEntry
import su.afk.yummy.tv.core.storage.collection.CollectionCatalogItemEntry
import su.afk.yummy.tv.core.storage.collection.CollectionCatalogPageCache
import su.afk.yummy.tv.core.storage.collection.CollectionCatalogPageEntry
import su.afk.yummy.tv.core.storage.collection.CollectionDetailCache
import su.afk.yummy.tv.core.storage.collection.CollectionDetailEntry
import su.afk.yummy.tv.domain.collection.model.CollectionAnimeItem
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionSummary

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

internal fun List<CollectionSummary>.toCollectionCatalogPageCache(
    pageKey: String,
    language: String,
    limit: Int,
    offset: Int,
    cachedAt: Long,
): CollectionCatalogPageCache =
    CollectionCatalogPageCache(
        entry = CollectionCatalogPageEntry(
            pageKey = pageKey,
            language = language,
            pageLimit = limit,
            pageOffset = offset,
            cachedAt = cachedAt,
        ),
        items = mapIndexed { index, item ->
            CollectionCatalogItemEntry(
                pageKey = pageKey,
                position = index,
                collectionId = item.id,
                title = item.title,
                description = item.description,
                posterUrl = item.posterUrl,
            )
        },
    )

internal fun CollectionCatalogPageCache.toCollectionSummaries(): List<CollectionSummary> =
    items
        .sortedBy { it.position }
        .map {
            CollectionSummary(
                id = it.collectionId,
                title = it.title,
                description = it.description,
                posterUrl = it.posterUrl,
            )
        }
