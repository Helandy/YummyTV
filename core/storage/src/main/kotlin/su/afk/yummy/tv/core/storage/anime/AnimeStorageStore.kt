package su.afk.yummy.tv.core.storage.anime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.common.episode.normalizedEpisodeNumber
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.serverWatchProgressOf

internal class AnimeStorageStore(private val dao: AnimeStorageDao) : AnimeStorage {

    override suspend fun getDetails(animeId: Int, language: String): AnimeDetailsCache? =
        dao.getDetails(animeId, language)

    override suspend fun saveDetails(cache: AnimeDetailsCache) {
        dao.replaceDetails(cache)
    }

    override suspend fun deleteDetails(animeId: Int, language: String) {
        dao.deleteDetails(animeId, language)
    }

    override suspend fun expireAllVideos() {
        dao.expireAllVideos()
    }

    override suspend fun expireAllDetails() {
        dao.expireAllDetails()
    }

    override suspend fun getVideos(animeId: Int, language: String): AnimeVideosCache? =
        dao.getVideos(animeId, language)

    override suspend fun saveVideos(cache: AnimeVideosCache) {
        dao.replaceVideos(cache)
    }

    override fun observeServerWatchProgress(language: String): Flow<List<AnimeWatchProgress>> =
        dao.observeWatchedVideoEntries(language).map { entries ->
            entries.mapNotNull { it.toServerWatchProgress() }
        }

    override suspend fun getRecommendations(
        animeId: Int,
        language: String,
        fromAi: Boolean,
    ): AnimeRecommendationsCache? =
        dao.getRecommendations(animeId, language, fromAi)

    override suspend fun saveRecommendations(cache: AnimeRecommendationsCache) {
        dao.replaceRecommendations(cache)
    }

    override suspend fun getTrailers(animeId: Int, language: String): AnimeTrailersCache? =
        dao.getTrailers(animeId, language)

    override suspend fun saveTrailers(cache: AnimeTrailersCache) {
        dao.replaceTrailers(cache)
    }
}

private fun AnimeVideoEntry.toServerWatchProgress(): AnimeWatchProgress? =
    serverWatchProgressOf(
        animeId = animeId,
        episode = episode.normalizedEpisodeNumber(),
        videoId = videoId,
        episodeUrl = iframeUrl,
        watchedEndTimeSeconds = watchedEndTimeSeconds,
        watchedDateSeconds = watchedDateSeconds,
        durationSeconds = durationSeconds,
        playerName = player,
        dubbing = dubbing,
    )
