package su.afk.yummy.tv.data.player.repository

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.data.player.mapper.toDomain
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import javax.inject.Inject

internal class DefaultWatchProgressRepository @Inject constructor(
    private val store: WatchProgressStorage,
) : WatchProgressRepository {

    override suspend fun get(animeId: Int, episode: String): AnimeWatchProgress? =
        store.get(animeId, episode)?.toDomain()

    override suspend fun getByAnime(animeId: Int): List<AnimeWatchProgress> =
        store.observeByAnimeId(animeId).first().map { it.toDomain() }

    override suspend fun save(
        animeId: Int,
        episode: String,
        videoId: Int,
        episodeUrl: String,
        positionMs: Long,
        durationMs: Long,
        updatedAt: Long,
        animeTitle: String,
        posterUrl: String,
        playerName: String,
        dubbing: String,
        screenshotUrl: String,
    ) = store.save(
        animeId = animeId,
        episode = episode,
        videoId = videoId,
        episodeUrl = episodeUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
        animeTitle = animeTitle,
        posterUrl = posterUrl,
        playerName = playerName,
        dubbing = dubbing,
        screenshotUrl = screenshotUrl,
    )

    override suspend fun saveContinueTarget(
        animeId: Int,
        episode: String,
        videoId: Int,
        episodeUrl: String,
        updatedAt: Long,
        animeTitle: String,
        posterUrl: String,
        playerName: String,
        dubbing: String,
        screenshotUrl: String,
    ) = store.saveContinueTarget(
        animeId = animeId,
        episode = episode,
        videoId = videoId,
        episodeUrl = episodeUrl,
        updatedAt = updatedAt,
        animeTitle = animeTitle,
        posterUrl = posterUrl,
        playerName = playerName,
        dubbing = dubbing,
        screenshotUrl = screenshotUrl,
    )

    override suspend fun delete(animeId: Int, episode: String) = store.delete(animeId, episode)

    override suspend fun suppressContinueWatchingDisplay(animeId: Int, suppressedAt: Long) =
        store.suppressContinueWatchingDisplay(animeId, suppressedAt)

    override suspend fun allMeaningfulVideoProgress(): List<AnimeWatchProgress> =
        store.allMeaningfulVideoProgress().map { it.toDomain() }
}
