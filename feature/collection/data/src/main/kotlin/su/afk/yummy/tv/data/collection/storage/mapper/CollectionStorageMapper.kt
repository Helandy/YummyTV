package su.afk.yummy.tv.data.collection.storage.mapper

import su.afk.yummy.tv.core.storage.collection.CollectionAnimeItemEntry
import su.afk.yummy.tv.core.storage.collection.CollectionCatalogItemEntry
import su.afk.yummy.tv.core.storage.collection.CollectionCatalogPageCache
import su.afk.yummy.tv.core.storage.collection.CollectionCatalogPageEntry
import su.afk.yummy.tv.core.storage.collection.CollectionDetailCache
import su.afk.yummy.tv.core.storage.collection.CollectionDetailEntry
import su.afk.yummy.tv.core.utils.network.toHttpsUrl
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionPosterDto
import su.afk.yummy.tv.domain.collection.model.CollectionAnimeItem
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionSummary
import su.afk.yummy.tv.domain.collection.model.CollectionSummaryPage
import su.afk.yummy.tv.domain.collection.model.CollectionVote

internal fun YaniCollectionDetailDto.toCollectionDetailCache(
    fallbackId: Int,
    language: String,
    cachedAt: Long,
): CollectionDetailCache =
    CollectionDetailCache(
        entry = CollectionDetailEntry(
            collectionId = id ?: fallbackId,
            language = language,
            ownerId = owner?.id ?: 0,
            title = title,
            description = description,
            isPublic = isPublic,
            ownerNickname = owner?.nickname.orEmpty(),
            ownerAvatarUrl = (owner?.avatars?.big ?: owner?.avatars?.small)?.toHttpsUrl(),
            commentsCount = commentsCount.coerceAtLeast(0),
            views = views,
            posterUrl = posterPreviews.firstOrNull()?.toUrl(),
            likes = likes?.likes?.coerceAtLeast(0) ?: 0,
            dislikes = likes?.dislikes?.coerceAtLeast(0) ?: 0,
            vote = CollectionVote.fromApi(likes?.vote).apiValue,
            cachedAt = cachedAt,
        ),
        items = animes.mapNotNull { item ->
            val animeId = item.animeId ?: return@mapNotNull null
            animeId to item
        }.mapIndexed { index, (animeId, item) ->
            CollectionAnimeItemEntry(
                collectionId = id ?: fallbackId,
                language = language,
                position = index,
                animeId = animeId,
                title = item.title,
                description = item.description,
                posterUrl = item.poster?.toUrl(),
                rating = item.rating?.average?.takeIf { it > 0.0 },
                year = item.year?.takeIf { it > 0 },
            )
        },
    )

internal fun CollectionDetailCache.toCollectionDetail(): CollectionDetail =
    CollectionDetail(
        id = entry.collectionId,
        ownerId = entry.ownerId,
        title = entry.title,
        description = entry.description,
        isPublic = entry.isPublic,
        ownerNickname = entry.ownerNickname,
        ownerAvatarUrl = entry.ownerAvatarUrl,
        commentsCount = entry.commentsCount,
        views = entry.views,
        posterUrl = entry.posterUrl,
        likesCount = entry.likes,
        dislikesCount = entry.dislikes,
        vote = CollectionVote.fromApi(entry.vote),
        animes = items
            .sortedBy { it.position }
            // Один тайтл может оказаться в подборке дважды — в гриде это дубль ключа и краш.
            .distinctBy { it.animeId }
            .map {
                CollectionAnimeItem(
                    id = it.animeId,
                    title = it.title,
                    description = it.description,
                    posterUrl = it.posterUrl,
                    rating = it.rating,
                    year = it.year,
                )
            },
    )

internal fun List<YaniCollectionDetailDto>.toCollectionCatalogPageCache(
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
        items = mapNotNull { item ->
            val id = item.id ?: return@mapNotNull null
            val title = item.title.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Triple(id, title, item)
        }.mapIndexed { index, (id, title, item) ->
            CollectionCatalogItemEntry(
                pageKey = pageKey,
                position = index,
                collectionId = id,
                title = title,
                description = item.description,
                posterUrl = item.posterPreviews.firstOrNull()?.toUrl(),
                likes = item.likes?.likes?.coerceAtLeast(0) ?: 0,
                views = item.views.coerceAtLeast(0),
                posterUrls = item.catalogPosterUrls().joinToString(POSTER_URLS_SEPARATOR),
            )
        },
    )

/** Постеры из `poster_previews`, а если их меньше [CATALOG_POSTER_COUNT] — добираем постерами `animes`. */
private fun YaniCollectionDetailDto.catalogPosterUrls(): List<String> =
    (posterPreviews.asSequence() + animes.asSequence().mapNotNull { it.poster })
        .mapNotNull { it.toUrl() }
        .distinct()
        .take(CATALOG_POSTER_COUNT)
        .toList()

private const val CATALOG_POSTER_COUNT = 3
private const val POSTER_URLS_SEPARATOR = "\n"

private fun YaniCollectionPosterDto.toUrl(): String? =
    (big ?: medium ?: fullsize ?: small)?.toHttpsUrl()

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
                viewsCount = it.views,
                posterUrls = it.posterUrls.split(POSTER_URLS_SEPARATOR).filter(String::isNotBlank),
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
