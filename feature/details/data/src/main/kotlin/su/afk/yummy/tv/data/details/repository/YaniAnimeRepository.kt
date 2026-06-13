package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.settings.withYaniContentLanguage
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.anime.isFresh
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeVideosDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationsDto
import su.afk.yummy.tv.data.details.dto.YaniTrailersResponseDto
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
    private val cache: CacheStore,
    private val animeStorage: AnimeStorageStore,
    private val json: Json,
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
                ?: readLegacyDetails(animeId, language, languageCode)
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
                ?: readLegacyVideos(animeId, language, languageCode)
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
                    ?: readLegacyRecommendations(animeId, language, languageCode, fromAi)
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
                    ?: readLegacyTrailers(animeId, language, languageCode)
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

    private suspend fun readLegacyDetails(
        animeId: Int,
        language: YaniContentLanguage,
        languageCode: String,
    ): AnimeDetails? {
        val cached = cache.getCached<YaniAnimeDetailsDto>(
            key = animeDetailsCacheKey(animeId, language),
            deserialize = { json.decodeFromString(it) },
            isValid = { it.response.animeId != null },
        ) ?: return null

        val details = cached.value.toAnimeDetails()
        animeStorage.saveDetails(
            details.toAnimeDetailsCache(
                language = languageCode,
                cachedAt = cached.cachedAt,
            )
        )
        return details
    }

    private suspend fun readLegacyVideos(
        animeId: Int,
        language: YaniContentLanguage,
        languageCode: String,
    ): List<AnimeVideo>? {
        val cached = cache.getCached<YaniAnimeVideosDto>(
            key = animeVideosCacheKey(animeId, language),
            deserialize = { json.decodeFromString(it) },
            isValid = { it.response.isNotEmpty() },
        ) ?: return null

        val videos = cached.value.response.map { it.toAnimeVideo() }
        animeStorage.saveVideos(
            videos.toAnimeVideosCache(
                animeId = animeId,
                language = languageCode,
                cachedAt = cached.cachedAt,
            )
        )
        return videos
    }

    private suspend fun readLegacyRecommendations(
        animeId: Int,
        language: YaniContentLanguage,
        languageCode: String,
        fromAi: Boolean,
    ): List<AnimeRecommendation>? {
        val cached = cache.getCached<YaniRecommendationsDto>(
            key = animeRecommendationsCacheKey(animeId, fromAi, language),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val recommendations = cached.value.response.mapNotNull { it.toAnimeRecommendation() }
        animeStorage.saveRecommendations(
            recommendations.toAnimeRecommendationsCache(
                animeId = animeId,
                language = languageCode,
                fromAi = fromAi,
                cachedAt = cached.cachedAt,
            )
        )
        return recommendations
    }

    private suspend fun readLegacyTrailers(
        animeId: Int,
        language: YaniContentLanguage,
        languageCode: String,
    ): List<AnimeTrailer>? {
        val cached = cache.getCached<YaniTrailersResponseDto>(
            key = animeTrailersCacheKey(animeId, language),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val trailers =
            cached.value.response.map { AnimeTrailer(iframeUrl = it.iframeUrl.toHttpsUrl()) }
        animeStorage.saveTrailers(
            trailers.toAnimeTrailersCache(
                animeId = animeId,
                language = languageCode,
                cachedAt = cached.cachedAt,
            )
        )
        return trailers
    }

    private fun animeDetailsCacheKey(animeId: Int, language: YaniContentLanguage): String =
        "anime_details_v2_$animeId".withYaniContentLanguage(language)

    private fun animeVideosCacheKey(animeId: Int, language: YaniContentLanguage): String =
        "anime_videos_$animeId".withYaniContentLanguage(language)

    private fun animeRecommendationsCacheKey(
        animeId: Int,
        fromAi: Boolean,
        language: YaniContentLanguage,
    ): String =
        "anime_recommendations_${fromAi}_$animeId".withYaniContentLanguage(language)

    private fun animeTrailersCacheKey(animeId: Int, language: YaniContentLanguage): String =
        "anime_trailers_$animeId".withYaniContentLanguage(language)
}
