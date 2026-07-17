package su.afk.yummy.tv.data.posts.repository

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.network.YaniApiJson
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.document.DocumentCacheStore
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.posts.dto.*
import su.afk.yummy.tv.data.posts.network.YaniPostsApi
import su.afk.yummy.tv.domain.posts.model.*
import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import javax.inject.Inject

class YaniPostsRepository @Inject constructor(
    private val api: YaniPostsApi,
    private val cache: DocumentCacheStore,
    private val settingsStore: SettingsStore,
) : PostsRepository {
    override suspend fun categories() = cached<YaniPostCategoriesResponseDto>(
        key = "categories",
        ttlMs = POST_CATEGORIES_TTL_MS,
    ) { api.categories() }.response.map(YaniPostCategoryDto::domain)

    override suspend fun posts(category: String?, sort: String, limit: Int, skip: Int) =
        cached<YaniPostsResponseDto>(
            key = "feed:${category.orEmpty()}:$sort:$limit:$skip",
            ttlMs = POST_FEED_TTL_MS,
        ) { api.posts(category, sort, limit, skip) }.response.map(YaniPostSummaryDto::domain)

    override suspend fun details(postId: Int) = cached<YaniPostDetailsResponseDto>(
        key = "detail:$postId",
        ttlMs = POST_DETAIL_TTL_MS,
    ) { api.details(postId) }.response.domain()

    override suspend fun vote(postId: Int, vote: PostVote): PostReaction {
        val result = api.vote(postId, vote.action).response.domain(vote)
        cache.deleteUserNamespace(POST_CACHE_NAMESPACE)
        return result
    }

    override suspend fun removeVote(postId: Int): PostReaction {
        val result = api.removeVote(postId).response.domain(PostVote.NONE)
        cache.deleteUserNamespace(POST_CACHE_NAMESPACE)
        return result
    }

    private suspend inline fun <reified T> cached(
        key: String,
        ttlMs: Long,
        crossinline fetch: suspend () -> T,
    ): T = cache.getOrFetch(
        cacheKey = cachePrefix() + key,
        ttlMs = ttlMs,
        decode = { YaniApiJson.decodeFromString<T>(it) },
        encode = { YaniApiJson.encodeToString(it) },
        fetch = { fetch() },
    )

    private suspend fun cachePrefix(): String {
        val userId = settingsStore.yaniUserId.first().coerceAtLeast(0)
        val language = settingsStore.yaniContentLanguage.first().apiCode
        return "user:$userId:$POST_CACHE_NAMESPACE:$language:"
    }
}

private const val POST_FEED_TTL_MS = 2 * 60 * 1000L
private const val POST_DETAIL_TTL_MS = 5 * 60 * 1000L
private const val POST_CATEGORIES_TTL_MS = 6 * 60 * 60 * 1000L
private const val POST_CACHE_NAMESPACE = "posts"

private fun YaniPostCategoryDto.domain() = PostCategory(id, title, uri)
private fun YaniPostAuthorDto.domain() = PostAuthor(
    id,
    nickname,
    (avatars?.full ?: avatars?.big ?: avatars?.small)?.toHttpsUrl(),
)

private fun YaniPostSummaryDto.domain() = PostSummary(
    id,
    title,
    previewImage?.toHttpsUrl(),
    contentPreview,
    user.domain(),
    category.domain(),
    createdAt,
)

private fun YaniPostVoteResultDto.domain(vote: PostVote) = PostReaction(likes, dislikes, vote)
private fun YaniPostDetailsDto.domain() = PostDetails(
    id = id,
    title = title,
    contentHtml = content,
    previewImageUrl = previewImage?.toHttpsUrl(),
    author = user.domain(),
    category = category.domain(),
    createdAt = createdAt,
    editedAt = editedAt,
    relatedAnime = animes.map {
        RelatedPostAnime(
            it.animeId,
            it.title,
            (it.poster?.big ?: it.poster?.medium)?.toHttpsUrl(),
            it.year,
            it.rating?.average,
        )
    },
    reaction = PostReaction(
        likes.likes,
        likes.dislikes,
        PostVote.entries.firstOrNull { it.action == likes.vote } ?: PostVote.NONE),
    views = views,
    comments = comments,
)
