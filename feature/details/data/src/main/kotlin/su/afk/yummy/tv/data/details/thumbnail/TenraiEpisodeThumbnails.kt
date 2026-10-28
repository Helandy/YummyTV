package su.afk.yummy.tv.data.details.thumbnail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeEpisodeThumbnailsUseCase
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Превью серий из Tenrai. Карта «номер серии → ссылка» тайтла грузится один раз на тайтл и держится в памяти:
 * лента «Продолжить» и история просят кадры десятков тайтлов разом, а лимит Tenrai — 4 запроса в секунду.
 * Параллельные запросы одного тайтла делят один [Deferred] на [IoApplicationScope]; пустой результат
 * (сбой или нет данных) помнится только [EMPTY_TTL_MS], чтобы один таймаут не лишал тайтл кадров до перезапуска.
 */
@Singleton
class TenraiEpisodeThumbnails @Inject constructor(
    private val getAnimeEpisodeThumbnails: GetAnimeEpisodeThumbnailsUseCase,
    @IoApplicationScope private val scope: CoroutineScope,
    private val clock: AppClock,
) {

    private class Entry(val thumbnails: Deferred<Map<String, String>>, val createdAtMs: Long)

    private val entries = ConcurrentHashMap<Int, Entry>()

    /** Ссылка на кадр серии или `null`, если серия не опознана, кадра нет или он не загрузился. */
    suspend fun previewUrl(animeId: Int, episode: String): String? =
        if (animeId > 0 && episode.isNotBlank()) thumbnailsOf(animeId)[episode.episodeGroupKey()] else null

    private suspend fun thumbnailsOf(animeId: Int): Map<String, String> {
        val now = clock.nowMillis()
        val entry = entries.compute(animeId) { _, current ->
            if (current != null && !current.isStaleEmpty(now)) {
                current
            } else {
                Entry(scope.async { getAnimeEpisodeThumbnails(animeId) }, now)
            }
        }!!
        return entry.thumbnails.await()
    }

    private fun Entry.isStaleEmpty(now: Long): Boolean =
        thumbnails.isCompleted && now - createdAtMs > EMPTY_TTL_MS && thumbnails.getCompleted().isEmpty()

    private companion object {
        const val EMPTY_TTL_MS = 60_000L
    }
}
