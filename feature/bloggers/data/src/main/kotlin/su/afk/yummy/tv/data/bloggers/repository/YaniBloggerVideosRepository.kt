package su.afk.yummy.tv.data.bloggers.repository

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.network.YaniApiJson
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.document.DocumentCacheStore
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.bloggers.dto.BloggerDetailsDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerDetailsResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoCategoryDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoReactionDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideosResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggersResponseDto
import su.afk.yummy.tv.data.bloggers.network.YaniBloggerVideosApi
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerDetails
import su.afk.yummy.tv.domain.bloggers.model.BloggerDirectory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoReaction
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

class YaniBloggerVideosRepository @Inject constructor(
    private val api: YaniBloggerVideosApi,
    private val cache: DocumentCacheStore,
    private val settingsStore: SettingsStore,
) :
    BloggerVideosRepository {
    override suspend fun getVideos(
        category: String,
        bloggerId: Int?,
        sort: BloggerVideoSort,
        limit: Int,
        offset: Int
    ) = cached<BloggerVideosResponseDto>(
        key = "videos:$category:${bloggerId ?: 0}:${sort.apiValue}:$limit:$offset",
        ttlMs = BLOGGER_FEED_TTL_MS,
    ) { api.getVideos(category, bloggerId, sort.apiValue, limit, offset) }
        .response.map { it.toDomain() }

    override suspend fun getAnimeVideos(animeId: Int, limit: Int, offset: Int) =
        cached<BloggerVideosResponseDto>(
            key = "anime:$animeId:$limit:$offset",
            ttlMs = BLOGGER_FEED_TTL_MS,
        ) { api.getAnimeVideos(animeId, limit, offset) }.response.map { it.toDomain() }

    override suspend fun getDirectory(limit: Int): BloggerDirectory =
        cached<BloggersResponseDto>(
            key = "directory:$limit",
            ttlMs = BLOGGER_DIRECTORY_TTL_MS,
        ) { api.getDirectory(limit) }.response.let { dto ->
            BloggerDirectory(
                dto.categories.map { it.toDomain() },
                dto.bloggers.map { it.toDomain() })
        }

    override suspend fun getBlogger(id: Int) = cached<BloggerDetailsResponseDto>(
        key = "blogger:$id",
        ttlMs = BLOGGER_DETAIL_TTL_MS,
    ) { api.getBlogger(id) }.response.toDomain()

    override suspend fun getVideo(id: Int) = cached<BloggerVideoResponseDto>(
        key = "video:$id",
        ttlMs = BLOGGER_DETAIL_TTL_MS,
    ) { api.getVideo(id) }.response.toDomain()

    override suspend fun setSubscribed(id: Int, subscribed: Boolean): Int {
        val result = (if (subscribed) api.subscribe(id) else api.unsubscribe(id))
            .response.subscriptions
        cache.deleteUserNamespace(BLOGGER_CACHE_NAMESPACE)
        return result
    }

    override suspend fun setVideoVote(id: Int, vote: BloggerVideoVote): BloggerVideoReaction {
        val result =
            (if (vote == BloggerVideoVote.NONE) api.removeVote(id) else api.vote(
                id,
                requireNotNull(vote.apiValue)
            ))
                .response.toDomain()
        cache.deleteUserNamespace(BLOGGER_CACHE_NAMESPACE)
        return result
    }

    private suspend inline fun <reified T> cached(
        key: String,
        ttlMs: Long,
        crossinline fetch: suspend () -> T,
    ): T = cache.getOrFetch(
        cacheKey = cachePrefix() + key,
        ttlMs = ttlMs,
        decode = YaniApiJson::decodeFromString,
        encode = YaniApiJson::encodeToString,
        fetch = { fetch() },
    )

    private suspend fun cachePrefix(): String {
        val userId = settingsStore.yaniUserId.first().coerceAtLeast(0)
        val language = settingsStore.yaniContentLanguage.first().apiCode
        return "user:$userId:$BLOGGER_CACHE_NAMESPACE:$language:"
    }
}

private const val BLOGGER_FEED_TTL_MS = 2 * 60 * 1000L
private const val BLOGGER_DETAIL_TTL_MS = 5 * 60 * 1000L
private const val BLOGGER_DIRECTORY_TTL_MS = 6 * 60 * 60 * 1000L
private const val BLOGGER_CACHE_NAMESPACE = "bloggers"

private fun BloggerVideoDto.toDomain() = BloggerVideo(
    id = id,
    title = title,
    description = descriptions.small.ifBlank { descriptions.big },
    previewUrl = (previews.big ?: previews.small)?.toHttpsUrl(),
    iframeUrl = iframeUrl.toHttpsUrl(),
    publishedAt = publishDate,
    views = views,
    hasSpoiler = hasSpoiler,
    category = category.toDomain(),
    creator = creator.toDomain(),
    reaction = likes.toDomain(),
    commentsCount = commentsCount,
)

private fun BloggerVideoCategoryDto.toDomain() = BloggerVideoCategory(id, title)
private fun BloggerDto.toDomain() =
    Blogger(id, nickname, (avatars.big ?: avatars.small)?.toHttpsUrl())

private fun BloggerDetailsDto.toDomain() = BloggerDetails(
    id = id,
    nickname = nickname,
    avatarUrl = (avatars.full ?: avatars.big ?: avatars.small)?.toHttpsUrl(),
    subscribers = subscriptions,
    videosCount = videosCount,
    isSubscribed = isSubscribed,
    categories = categories.map { it.toDomain() },
)

private fun BloggerVideoReactionDto.toDomain() = BloggerVideoReaction(
    likes = likes,
    dislikes = dislikes,
    vote = when (vote) {
        1 -> BloggerVideoVote.LIKE
        -1 -> BloggerVideoVote.DISLIKE
        else -> BloggerVideoVote.NONE
    },
)
