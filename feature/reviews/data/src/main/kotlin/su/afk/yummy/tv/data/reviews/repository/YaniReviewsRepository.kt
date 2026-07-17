package su.afk.yummy.tv.data.reviews.repository

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import su.afk.yummy.tv.core.network.YaniApiJson
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.document.DocumentCacheStore
import su.afk.yummy.tv.data.reviews.dto.YaniReviewDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewResponseDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewsFeedResponseDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewsPageResponseDto
import su.afk.yummy.tv.data.reviews.network.YaniReviewsApi
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewDetails
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewAuthor
import su.afk.yummy.tv.domain.reviews.model.ReviewPage
import su.afk.yummy.tv.domain.reviews.model.ReviewRating
import su.afk.yummy.tv.domain.reviews.model.ReviewRatingCategory
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class YaniReviewsRepository @Inject constructor(
    private val api: YaniReviewsApi,
    private val cache: DocumentCacheStore,
    private val animeStorage: AnimeStorageStore,
    private val settingsStore: SettingsStore,
) :
    ReviewsRepository {
    private val reviewAnimeIds = ConcurrentHashMap<Int, Int>()

    override suspend fun getReviews(
        sort: ReviewSort,
        limit: Int,
        offset: Int,
    ): ReviewPage = ReviewPage(
        cached<YaniReviewsFeedResponseDto>(
            key = "feed:${sort.apiValue}:$limit:$offset",
            ttlMs = REVIEW_FEED_TTL_MS,
        ) { api.getReviews(sort.apiValue, limit, offset) }.response.mapNotNull {
            rememberAnimeId(it)
            it.toSummaryOrNull()
        },
    )

    override suspend fun getAnimeReviews(
        animeId: Int,
        sort: ReviewSort,
        limit: Int,
        offset: Int
    ): ReviewPage {
        val page = cached<YaniReviewsPageResponseDto>(
            key = "anime:$animeId:${sort.apiValue}:$limit:$offset",
            ttlMs = REVIEW_FEED_TTL_MS,
        ) { api.getAnimeReviews(animeId, sort.apiValue, limit, offset) }.response
        page.reviews.forEach(::rememberAnimeId)
        return ReviewPage(page.reviews.mapNotNull { it.toSummaryOrNull() })
    }

    override suspend fun getReview(reviewId: Int): AnimeReviewDetails {
        val dto = cached<YaniReviewResponseDto>(
            key = "detail:$reviewId",
            ttlMs = REVIEW_DETAIL_TTL_MS,
        ) { api.getReview(reviewId) }.response
        rememberAnimeId(dto)
        return AnimeReviewDetails(
            review = dto.toSummaryOrNull() ?: error("Review not found"),
            animeTitle = dto.anime?.title.orEmpty(),
            animePosterUrl = dto.anime?.poster?.run { mega ?: huge ?: big ?: medium ?: small }
                .toHttps(),
            commentsCount = dto.commentsCount,
        )
    }

    override suspend fun delete(reviewId: Int): Boolean {
        val deleted = api.delete(reviewId).response
        if (deleted) {
            val language = settingsStore.yaniContentLanguage.first().apiCode
            val animeId = reviewAnimeIds.remove(reviewId)
            if (animeId != null) animeStorage.deleteDetails(animeId, language)
            else animeStorage.expireAllDetails()
            cache.deleteUserNamespace(REVIEW_CACHE_NAMESPACE)
        }
        return deleted
    }

    override suspend fun vote(reviewId: Int, vote: ReviewVote): ReviewReactions {
        val result = if (vote == ReviewVote.NONE) api.removeVote(reviewId).response else api.vote(
            reviewId,
            vote.apiValue
        ).response
        if (!result.success) error("Vote was not saved")
        cache.deleteUserNamespace(REVIEW_CACHE_NAMESPACE)
        return ReviewReactions(result.likes, result.dislikes, vote)
    }

    private fun rememberAnimeId(dto: YaniReviewDto) {
        if (dto.reviewId > 0 && dto.animeId > 0) reviewAnimeIds[dto.reviewId] = dto.animeId
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
        return "user:$userId:$REVIEW_CACHE_NAMESPACE:$language:"
    }

}

private const val REVIEW_FEED_TTL_MS = 2 * 60 * 1000L
private const val REVIEW_DETAIL_TTL_MS = 5 * 60 * 1000L
private const val REVIEW_CACHE_NAMESPACE = "reviews"

private fun YaniReviewDto.toSummaryOrNull(): AnimeReviewSummary? {
    if (reviewId <= 0) return null
    return AnimeReviewSummary(
        id = reviewId,
        animeId = animeId,
        status = when (type) {
            "waiting" -> ReviewStatus.WAITING; "declined" -> ReviewStatus.DECLINED; else -> ReviewStatus.APPROVED
        },
        author = ReviewAuthor(
            author.id ?: userId ?: 0,
            author.nickname ?: nickname.orEmpty(),
            author.avatars?.run { full ?: big ?: small }.toHttps()
        ),
        createdAtSeconds = createDate,
        updatedAtSeconds = updateDate,
        views = views,
        rating = rating?.let {
            ReviewRating(
                it.average?.jsonPrimitive?.doubleOrNull?.toInt(),
                it.category.orEmpty().map { (name, score) -> ReviewRatingCategory(name, score) })
        },
        reactions = ReviewReactions(
            likes.likes,
            likes.dislikes,
            ReviewVote.entries.firstOrNull { it.apiValue == likes.vote } ?: ReviewVote.NONE),
        html = textHtml.ifBlank { textPreview },
        checkComment = checkComment,
        commentable = commentable,
        animeTitle = anime?.title.orEmpty(),
        animePosterUrl = anime?.poster?.run { mega ?: huge ?: big ?: medium ?: small ?: fullsize }
            .toHttps(),
    )
}

private fun String?.toHttps(): String? = this?.trim()?.takeIf { it.isNotEmpty() }?.let {
    when {
        it.startsWith("//") -> "https:$it"; it.startsWith("http://") -> "https://${it.removePrefix("http://")}"; else -> it
    }
}
