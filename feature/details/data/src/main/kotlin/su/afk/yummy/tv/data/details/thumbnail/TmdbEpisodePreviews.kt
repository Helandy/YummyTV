package su.afk.yummy.tv.data.details.thumbnail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.model.anime.AnimeTmdbImages
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeTmdbImagesUseCase
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кадры серий из TMDB (приходят от YummyTV API вместе с названиями серий). Карта «номер серии → ссылка» тайтла
 * грузится один раз на тайтл и держится в памяти: лента «Продолжить» и история просят кадры десятков тайтлов разом.
 * Параллельные запросы одного тайтла делят один [Deferred] на [IoApplicationScope]; пустой результат (сбой или нет
 * данных) помнится только [EMPTY_TTL_MS], чтобы один таймаут не лишал тайтл кадров до перезапуска.
 *
 * API отдаёт кадры в w300, а карточки серий крупнее: мобильному берём w780, ТВ — w1280.
 */
@Singleton
class TmdbEpisodePreviews @Inject constructor(
    private val getAnimeTmdbImages: GetAnimeTmdbImagesUseCase,
    private val interfaceModePreferences: AppInterfaceModePreferences,
    @IoApplicationScope private val scope: CoroutineScope,
    private val clock: AppClock,
) {

    private class Entry(val previews: Deferred<AnimeTmdbImages>, val createdAtMs: Long)

    private val entries = ConcurrentHashMap<Int, Entry>()

    /** Ссылка на кадр серии или `null`, если серия не опознана, кадра нет или он не загрузился. */
    suspend fun previewUrl(animeId: Int, episode: String): String? {
        if (animeId <= 0 || episode.isBlank()) return null
        val url = previewsOf(animeId).episodePreviewUrls[episode.episodeGroupKey()] ?: return null
        val size = if (interfaceModePreferences.selectedMode == AppInterfaceMode.TV) TV_SIZE_SEGMENT else MOBILE_SIZE_SEGMENT
        return url.replace(SERVED_SIZE_SEGMENT, size)
    }

    /**
     * Своя копия кадра серии в AVIF (хост YummyTV) или `null`, если API её не назвал. Файла на хосте может ещё не быть:
     * тогда загрузка вернёт 404, и берётся следующий источник цепочки.
     */
    suspend fun mirrorUrl(animeId: Int, episode: String): String? {
        if (animeId <= 0 || episode.isBlank()) return null
        return previewsOf(animeId).episodeMirrorUrls[episode.episodeGroupKey()]
    }

    private suspend fun previewsOf(animeId: Int): AnimeTmdbImages {
        val now = clock.nowMillis()
        val entry = entries.compute(animeId) { _, current ->
            if (current != null && !current.isStaleEmpty(now)) {
                current
            } else {
                Entry(scope.async { getAnimeTmdbImages(animeId) }, now)
            }
        }!!
        return entry.previews.await()
    }

    private fun Entry.isStaleEmpty(now: Long): Boolean =
        previews.isCompleted && now - createdAtMs > EMPTY_TTL_MS && previews.getCompleted().let { it.episodePreviewUrls.isEmpty() && it.episodeMirrorUrls.isEmpty() }

    private companion object {
        const val EMPTY_TTL_MS = 60_000L
        const val SERVED_SIZE_SEGMENT = "/w300/"
        const val MOBILE_SIZE_SEGMENT = "/w780/"
        const val TV_SIZE_SEGMENT = "/w1280/"
    }
}
