package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.anime.isFresh
import su.afk.yummy.tv.data.details.mapper.toAnimeDetails
import su.afk.yummy.tv.data.details.mapper.toAnimeDetailsCache
import su.afk.yummy.tv.data.details.mapper.toAnimeRecommendation
import su.afk.yummy.tv.data.details.mapper.toAnimeRecommendations
import su.afk.yummy.tv.data.details.mapper.toAnimeRecommendationsCache
import su.afk.yummy.tv.data.details.mapper.toAnimeTrailers
import su.afk.yummy.tv.data.details.mapper.toAnimeTrailersCache
import su.afk.yummy.tv.data.details.mapper.toAnimeVideo
import su.afk.yummy.tv.data.details.mapper.toAnimeVideos
import su.afk.yummy.tv.data.details.mapper.toAnimeVideosCache
import su.afk.yummy.tv.data.details.mapper.toHttpsUrl
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository

private const val ANIME_DETAILS_TTL_MS = 24 * 60 * 60 * 1000L
private const val ANIME_VIDEOS_TTL_MS = 60 * 60 * 1000L
private const val ANIME_PUBLIC_EXTRAS_TTL_MS = 6 * 60 * 60 * 1000L

class YaniAnimeRepository(
    private val api: YaniAnimeApi,
    private val animeStorage: AnimeStorageStore,
    private val settingsStore: SettingsStore,
) : AnimeRepository {

    override suspend fun getAnimeDetails(animeId: Int): AnimeDetails = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = animeStorage.getDetails(animeId, languageCode)
        if (stored?.isFresh(ANIME_DETAILS_TTL_MS) == true) {
            return@withContext stored.toAnimeDetails()
        }

        try {
            fetchDetailsFromNetwork(animeId, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toAnimeDetails()
                ?: throw error
        }
    }

    override suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo> = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = animeStorage.getVideos(animeId, languageCode)
        if (stored?.isFresh(ANIME_VIDEOS_TTL_MS) == true) {
            return@withContext stored.toAnimeVideos()
        }

        try {
            fetchVideosFromNetwork(animeId, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toAnimeVideos()
                ?: throw error
        }
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
                return@withContext stored.toAnimeRecommendations()
            }

            try {
                fetchRecommendationsFromNetwork(animeId, languageCode, fromAi)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toAnimeRecommendations()
                    ?: emptyList()
            }
        }

    override suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = animeStorage.getTrailers(animeId, languageCode)
            if (stored?.isFresh(ANIME_PUBLIC_EXTRAS_TTL_MS) == true) {
                return@withContext stored.toAnimeTrailers()
            }

            try {
                fetchTrailersFromNetwork(animeId, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toAnimeTrailers()
                    ?: emptyList()
            }
        }

    private suspend fun fetchDetailsFromNetwork(
        animeId: Int,
        languageCode: String,
    ): AnimeDetails {
        val dto = api.getAnimeDetails(animeId)
        val details = dto.toAnimeDetails()
        if (dto.response.animeId != null) {
            animeStorage.saveDetails(
                details.toAnimeDetailsCache(
                    language = languageCode,
                    cachedAt = System.currentTimeMillis(),
                )
            )
        }
        return details
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
