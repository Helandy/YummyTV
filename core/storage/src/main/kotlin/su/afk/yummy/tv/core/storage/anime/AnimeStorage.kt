package su.afk.yummy.tv.core.storage.anime

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress

/** Абстракция над локальным кэшем деталей тайтла — позволяет подменять реализацию в тестах. */
interface AnimeStorage {

    suspend fun getDetails(animeId: Int, language: String): AnimeDetailsCache?

    suspend fun saveDetails(cache: AnimeDetailsCache)

    suspend fun deleteDetails(animeId: Int, language: String)

    suspend fun expireAllDetails()

    /** Помечает кэш видео устаревшим: `watched` и `subscribed` в нём привязаны к пользователю. */
    suspend fun expireAllVideos()

    suspend fun getVideos(animeId: Int, language: String): AnimeVideosCache?

    suspend fun saveVideos(cache: AnimeVideosCache)

    /** Серверный прогресс по всем тайтлам из кэша видео (`/videos` → `watched`), с обновлениями. */
    fun observeServerWatchProgress(language: String): Flow<List<AnimeWatchProgress>>

    suspend fun getRecommendations(
        animeId: Int,
        language: String,
        fromAi: Boolean,
    ): AnimeRecommendationsCache?

    suspend fun saveRecommendations(cache: AnimeRecommendationsCache)

    suspend fun getTrailers(animeId: Int, language: String): AnimeTrailersCache?

    suspend fun saveTrailers(cache: AnimeTrailersCache)
}
