package su.afk.yummy.tv.data.bloggers.repository

import su.afk.yummy.tv.core.storage.document.UserScopedCache
import su.afk.yummy.tv.data.bloggers.dto.BloggerDetailsResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideosResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggersResponseDto
import su.afk.yummy.tv.data.bloggers.mapper.toDomain
import su.afk.yummy.tv.data.bloggers.network.YaniBloggerVideosApi
import su.afk.yummy.tv.domain.bloggers.model.BloggerDirectory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoReaction
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

class YaniBloggerVideosRepository @Inject constructor(
    private val api: YaniBloggerVideosApi,
    private val cache: UserScopedCache,
) :
    BloggerVideosRepository {
    override suspend fun getVideos(
        category: String,
        bloggerId: Int?,
        sort: BloggerVideoSort,
        limit: Int,
        offset: Int,
        forceRefresh: Boolean,
    ) = cache.cached<BloggerVideosResponseDto>(
        namespace = BLOGGER_CACHE_NAMESPACE,
        key = "videos:$category:${bloggerId ?: 0}:${sort.apiValue}:$limit:$offset",
        ttlMs = BLOGGER_FEED_TTL_MS,
        forceRefresh = forceRefresh,
    ) { api.getVideos(category, bloggerId, sort.apiValue, limit, offset) }
        .response.map { it.toDomain() }

    override suspend fun cachedVideos(category: String, bloggerId: Int?, sort: BloggerVideoSort, limit: Int, offset: Int) =
        cache.read<BloggerVideosResponseDto>(BLOGGER_CACHE_NAMESPACE, "videos:$category:${bloggerId ?: 0}:${sort.apiValue}:$limit:$offset")
            ?.response?.map { it.toDomain() }

    override suspend fun getAnimeVideos(animeId: Int, limit: Int, offset: Int) =
        cache.cached<BloggerVideosResponseDto>(
            namespace = BLOGGER_CACHE_NAMESPACE,
            key = "anime:$animeId:$limit:$offset",
            ttlMs = BLOGGER_FEED_TTL_MS,
        ) { api.getAnimeVideos(animeId, limit, offset) }.response.map { it.toDomain() }

    override suspend fun getDirectory(limit: Int): BloggerDirectory =
        cache.cached<BloggersResponseDto>(
            namespace = BLOGGER_CACHE_NAMESPACE,
            key = "directory:$limit",
            ttlMs = BLOGGER_DIRECTORY_TTL_MS,
        ) { api.getDirectory(limit) }.response.let { dto ->
            BloggerDirectory(
                dto.categories.map { it.toDomain() },
                dto.bloggers.map { it.toDomain() })
        }

    override suspend fun getBlogger(id: Int) = cache.cached<BloggerDetailsResponseDto>(
        namespace = BLOGGER_CACHE_NAMESPACE,
        key = "blogger:$id",
        ttlMs = BLOGGER_DETAIL_TTL_MS,
    ) { api.getBlogger(id) }.response.toDomain()

    override suspend fun getVideo(id: Int) = cache.cached<BloggerVideoResponseDto>(
        namespace = BLOGGER_CACHE_NAMESPACE,
        key = "video:$id",
        ttlMs = BLOGGER_DETAIL_TTL_MS,
    ) { api.getVideo(id) }.response.toDomain()

    override suspend fun setSubscribed(id: Int, subscribed: Boolean): Int {
        val result = (if (subscribed) api.subscribe(id) else api.unsubscribe(id))
            .response.subscriptions
        // Подписка меняет только карточку блогера и директорию, не весь namespace.
        cache.delete(BLOGGER_CACHE_NAMESPACE, "blogger:$id")
        cache.deleteByPrefix(BLOGGER_CACHE_NAMESPACE, "directory:")
        return result
    }

    override suspend fun setVideoVote(id: Int, vote: BloggerVideoVote): BloggerVideoReaction {
        val result =
            (if (vote == BloggerVideoVote.NONE) api.removeVote(id) else api.vote(
                id,
                requireNotNull(vote.apiValue)
            ))
                .response.toDomain()
        // Голос затрагивает только конкретное видео, а не весь namespace.
        cache.delete(BLOGGER_CACHE_NAMESPACE, "video:$id")
        return result
    }
}

private const val BLOGGER_FEED_TTL_MS = 2 * 60 * 1000L
private const val BLOGGER_DETAIL_TTL_MS = 5 * 60 * 1000L
private const val BLOGGER_DIRECTORY_TTL_MS = 6 * 60 * 60 * 1000L
private const val BLOGGER_CACHE_NAMESPACE = "bloggers"
