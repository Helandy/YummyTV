package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.anime.isFresh
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.mapper.toAnimeDetails
import su.afk.yummy.tv.data.details.mapper.toAnimeRecommendation
import su.afk.yummy.tv.data.details.mapper.toAnimeVideo
import su.afk.yummy.tv.data.details.mapper.toHttpsUrl
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.storage.mapper.toAccountUserRatingEntry
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeDetailsCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeRecommendationsCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeTrailersCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeVideosCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeWatchProgress
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeWatchProgress
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeDetails as toStoredAnimeDetails
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeRecommendations as toStoredAnimeRecommendations
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeTrailers as toStoredAnimeTrailers
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeVideos as toStoredAnimeVideos

private const val ANIME_DETAILS_TTL_MS = 24 * 60 * 60 * 1000L
private const val ANIME_VIDEOS_TTL_MS = 5 * 60 * 1000L
private const val ANIME_PUBLIC_EXTRAS_TTL_MS = 6 * 60 * 60 * 1000L

class YaniAnimeRepository(
    private val api: YaniAnimeApi,
    private val animeStorage: AnimeStorageStore,
    private val accountStorage: AccountStorageStore,
    private val settingsStore: SettingsStore,
    private val watchProgressStore: WatchProgressStore,
) : AnimeRepository {

    override suspend fun getAnimeDetails(animeId: Int): AnimeDetails = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = animeStorage.getDetails(animeId, languageCode)
        if (stored?.isFresh(ANIME_DETAILS_TTL_MS) == true) {
            return@withContext stored.toStoredAnimeDetails()
        }

        try {
            fetchDetailsFromNetwork(animeId, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toStoredAnimeDetails()
                ?: throw error
        }
    }

    override suspend fun getCachedAnimeDetails(animeId: Int): AnimeDetails? =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            animeStorage.getDetails(animeId, language.apiCode)?.toStoredAnimeDetails()
        }

    override suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = animeStorage.getVideos(animeId, languageCode)
            if (stored?.isFresh(ANIME_VIDEOS_TTL_MS) == true) {
                return@withContext stored.toStoredAnimeVideos()
            }

            try {
                fetchVideosFromNetwork(animeId, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toStoredAnimeVideos()
                    ?: throw error
            }
        }

    override suspend fun refreshAnimeVideos(animeId: Int): List<AnimeVideo> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            fetchVideosFromNetwork(animeId, language.apiCode)
        }

    override suspend fun getCachedAnimeVideos(animeId: Int): List<AnimeVideo>? =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            animeStorage.getVideos(animeId, language.apiCode)?.toStoredAnimeVideos()
        }

    override suspend fun getAnimeRecommendations(
        animeId: Int,
        fromAi: Boolean
    ): List<AnimeRecommendation> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = animeStorage.getRecommendations(animeId, languageCode, fromAi)
            if (stored?.isFresh(ANIME_PUBLIC_EXTRAS_TTL_MS) == true) {
                return@withContext stored.toStoredAnimeRecommendations()
            }

            try {
                fetchRecommendationsFromNetwork(animeId, languageCode, fromAi)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toStoredAnimeRecommendations()
                    ?: emptyList()
            }
        }

    override suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = animeStorage.getTrailers(animeId, languageCode)
            if (stored?.isFresh(ANIME_PUBLIC_EXTRAS_TTL_MS) == true) {
                return@withContext stored.toStoredAnimeTrailers()
            }

            try {
                fetchTrailersFromNetwork(animeId, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toStoredAnimeTrailers()
                    ?: emptyList()
            }
        }

    override fun observeWatchProgress(animeId: Int): Flow<List<AnimeWatchProgress>> =
        watchProgressStore.observeByAnimeId(animeId)
            .map { entries -> entries.map { it.toAnimeWatchProgress() } }

    private suspend fun fetchDetailsFromNetwork(
        animeId: Int,
        languageCode: String,
    ): AnimeDetails {
        val dto = api.getAnimeDetails(animeId)
        val details = dto.toAnimeDetails()
        val cachedAt = System.currentTimeMillis()
        if (dto.response.animeId != null) {
            animeStorage.saveDetails(
                details.toAnimeDetailsCache(
                    language = languageCode,
                    cachedAt = cachedAt,
                )
            )
        }
        saveUserRatingFromDetails(dto, animeId, cachedAt)
        return details
    }

    private suspend fun saveUserRatingFromDetails(
        dto: YaniAnimeDetailsDto,
        animeId: Int,
        cachedAt: Long,
    ) {
        val userId = settingsStore.yaniUserId.first()
        if (userId <= 0 || dto.response.animeId == null) return

        accountStorage.saveUserRating(
            dto.response.user?.rating?.toInt()?.takeIf { it in 1..10 }.toAccountUserRatingEntry(
                userId = userId,
                animeId = animeId,
                cachedAt = cachedAt,
            )
        )
    }

    private suspend fun fetchVideosFromNetwork(
        animeId: Int,
        languageCode: String,
    ): List<AnimeVideo> {
        val dto = api.getAnimeVideos(animeId)
        val videos = dto.response.map { it.toAnimeVideo() }
        animeStorage.saveVideos(
            videos.toAnimeVideosCache(
                animeId = animeId,
                language = languageCode,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return videos
    }

    private suspend fun fetchRecommendationsFromNetwork(
        animeId: Int,
        languageCode: String,
        fromAi: Boolean,
    ): List<AnimeRecommendation> {
        val recommendations = api.getAnimeRecommendations(animeId, fromAi)
            .response
            .mapNotNull { it.toAnimeRecommendation() }
        animeStorage.saveRecommendations(
            recommendations.toAnimeRecommendationsCache(
                animeId = animeId,
                language = languageCode,
                fromAi = fromAi,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return recommendations
    }

    private suspend fun fetchTrailersFromNetwork(
        animeId: Int,
        languageCode: String,
    ): List<AnimeTrailer> {
        val trailers = api.getAnimeTrailers(animeId)
            .response
            .map { AnimeTrailer(iframeUrl = it.iframeUrl.toHttpsUrl()) }
        animeStorage.saveTrailers(
            trailers.toAnimeTrailersCache(
                animeId = animeId,
                language = languageCode,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return trailers
    }
}
