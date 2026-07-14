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
import su.afk.yummy.tv.domain.collection.model.CollectionSummaryPage
import su.afk.yummy.tv.domain.collection.model.CollectionVote

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
            likes = likesCount,
            dislikes = dislikesCount,
            vote = vote.apiValue,
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
                year = item.year,
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
        likesCount = entry.likes,
        dislikesCount = entry.dislikes,
        vote = CollectionVote.fromApi(entry.vote),
        animes = items
            .sortedBy { it.position }
            .map {
                CollectionAnimeItem(
                    id = it.animeId,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    rating = it.rating,
                    year = it.year,
                )
            },
    )

internal fun List<CollectionSummary>.toCollectionCatalogPageCache(
    pageKey: String,
    language: String,
    limit: Int,
    offset: Int,
    responseSize: Int,
    cachedAt: Long,
): CollectionCatalogPageCache =
    CollectionCatalogPageCache(
        entry = CollectionCatalogPageEntry(
            pageKey = pageKey,
            language = language,
            pageLimit = limit,
            pageOffset = offset,
            responseSize = responseSize,
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
                likes = item.likesCount,
            )
        },
    )

internal fun CollectionCatalogPageCache.toCollectionSummaryPage(): CollectionSummaryPage =
    items
        .sortedBy { it.position }
        .map {
            CollectionSummary(
                id = it.collectionId,
                title = it.title,
                description = it.description,
                posterUrl = it.posterUrl,
                likesCount = it.likes,
            )
        }
        .let { summaries ->
            val responseSize = entry.responseSize.takeIf { it > 0 }
                ?: if (summaries.isNotEmpty()) entry.pageLimit else 0
            CollectionSummaryPage(
                items = summaries,
                nextOffset = entry.pageOffset + responseSize,
                canLoadMore = responseSize >= entry.pageLimit,
            )
        }
