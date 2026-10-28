package su.afk.yummy.tv.domain.player.repository

import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress

/** Читает/пишет локальный прогресс просмотра серий и управляет видимостью Continue Watching. */
interface WatchProgressRepository {

    suspend fun get(animeId: Int, episode: String): AnimeWatchProgress?

    /** Локальный прогресс всех серий тайтла. */
    suspend fun getByAnime(animeId: Int): List<AnimeWatchProgress>

    suspend fun save(
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
    )

    suspend fun saveContinueTarget(
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
    )

    /** Удаляет локальный прогресс серии, например при снятии ручной отметки о просмотре. */
    suspend fun delete(animeId: Int, episode: String)

    suspend fun suppressContinueWatchingDisplay(animeId: Int, suppressedAt: Long)

    suspend fun allMeaningfulVideoProgress(): List<AnimeWatchProgress>
}
