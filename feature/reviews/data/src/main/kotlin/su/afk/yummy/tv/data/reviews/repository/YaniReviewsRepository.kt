package su.afk.yummy.tv.data.reviews.repository

import su.afk.yummy.tv.core.storage.document.UserScopedCache
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.data.reviews.dto.YaniReviewDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewResponseDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewsFeedResponseDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewsPageResponseDto
import su.afk.yummy.tv.data.reviews.mapper.toDetailsOrNull
import su.afk.yummy.tv.data.reviews.mapper.toReviewPage
import su.afk.yummy.tv.data.reviews.network.YaniReviewsApi
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewDetails
import su.afk.yummy.tv.domain.reviews.model.ReviewPage
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class YaniReviewsRepository @Inject constructor(
    private val api: YaniReviewsApi,
    private val cache: UserScopedCache,
    private val animeStorage: AnimeStorage,
    private val settingsStore: YaniAccountSettingsStore,
) :
    ReviewsRepository {
    private val reviewAnimeIds = ConcurrentHashMap<Int, Int>()

    override suspend fun getReviews(
        sort: ReviewSort,
        limit: Int,
        offset: Int,
    ): ReviewPage {
        val reviews = cache.cached<YaniReviewsFeedResponseDto>(
            namespace = REVIEW_CACHE_NAMESPACE,
            key = "feed:${sort.apiValue}:$limit:$offset",
            ttlMs = REVIEW_FEED_TTL_MS,
        ) { api.getReviews(sort.apiValue, limit, offset) }.response
        reviews.forEach(::rememberAnimeId)
        return reviews.toReviewPage()
    }

    override suspend fun getAnimeReviews(
        animeId: Int,
        sort: ReviewSort,
        limit: Int,
        offset: Int
    ): ReviewPage {
        val page = cache.cached<YaniReviewsPageResponseDto>(
            namespace = REVIEW_CACHE_NAMESPACE,
            key = "anime:$animeId:${sort.apiValue}:$limit:$offset",
            ttlMs = REVIEW_FEED_TTL_MS,
        ) { api.getAnimeReviews(animeId, sort.apiValue, limit, offset) }.response
        page.reviews.forEach(::rememberAnimeId)
        return page.reviews.toReviewPage()
    }

    override suspend fun getReview(reviewId: Int): AnimeReviewDetails {
        val dto = cache.cached<YaniReviewResponseDto>(
            namespace = REVIEW_CACHE_NAMESPACE,
            key = "detail:$reviewId",
            ttlMs = REVIEW_DETAIL_TTL_MS,
        ) { api.getReview(reviewId) }.response
        rememberAnimeId(dto)
        return dto.toDetailsOrNull() ?: error("Review not found")
    }

    override suspend fun delete(reviewId: Int): Boolean {
        val deleted = api.delete(reviewId).response
        if (deleted) {
            val language = settingsStore.currentLanguageCode()
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
        // Голос затрагивает только конкретную рецензию: чистим её деталь, а не весь namespace.
        // Ленты имеют короткий TTL, а список применяет оптимистичный override поверх кэша.
        cache.delete(REVIEW_CACHE_NAMESPACE, "detail:$reviewId")
        return ReviewReactions(result.likes, result.dislikes, vote)
    }

    private fun rememberAnimeId(dto: YaniReviewDto) {
        if (dto.reviewId > 0 && dto.animeId > 0) reviewAnimeIds[dto.reviewId] = dto.animeId
    }

}

private const val REVIEW_FEED_TTL_MS = 2 * 60 * 1000L
private const val REVIEW_DETAIL_TTL_MS = 5 * 60 * 1000L
private const val REVIEW_CACHE_NAMESPACE = "reviews"

