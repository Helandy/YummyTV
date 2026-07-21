package su.afk.yummy.tv.domain.watching.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import javax.inject.Inject

internal class ContinueWatchingVideoLoader @Inject constructor(
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase,
) {

    suspend fun load(animeId: Int, refresh: Boolean): List<AnimeVideo> {
        if (animeId == 0) return emptyList()
        return if (refresh) loadRefreshedOrCached(animeId) else loadCached(animeId)
    }

    private suspend fun loadRefreshedOrCached(animeId: Int): List<AnimeVideo> = try {
        refreshAnimeVideos(animeId)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        loadCached(animeId)
    }

    private suspend fun loadCached(animeId: Int): List<AnimeVideo> = try {
        getAnimeVideos(animeId)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        emptyList()
    }
}
