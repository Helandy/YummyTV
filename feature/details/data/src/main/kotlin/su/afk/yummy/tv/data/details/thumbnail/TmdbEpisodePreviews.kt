package su.afk.yummy.tv.data.details.thumbnail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
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
 * API отдаёт кадры в w300: на мобильном этого хватает, на ТВ карточки крупные, поэтому там берём w780.
 */
@Singleton
class TmdbEpisodePreviews @Inject constructor(
    private val getAnimeTmdbImages: GetAnimeTmdbImagesUseCase,
    private val interfaceModePreferences: AppInterfaceModePreferences,
    @IoApplicationScope private val scope: CoroutineScope,
    private val clock: AppClock,
) {

    private class Entry(val previews: Deferred<Map<String, String>>, val createdAtMs: Long)

    private val entries = ConcurrentHashMap<Int, Entry>()

    /** Ссылка на кадр серии или `null`, если серия не опознана, кадра нет или он не загрузился. */
    suspend fun previewUrl(animeId: Int, episode: String): String? {
        if (animeId <= 0 || episode.isBlank()) return null
        val url = previewsOf(animeId)[episode.episodeGroupKey()] ?: return null
        return if (interfaceModePreferences.selectedMode == AppInterfaceMode.TV) url.withTvSize() else url
    }

    private suspend fun previewsOf(animeId: Int): Map<String, String> {
        val now = clock.nowMillis()
        val entry = entries.compute(animeId) { _, current ->
            if (current != null && !current.isStaleEmpty(now)) {
                current
            } else {
                Entry(scope.async { getAnimeTmdbImages(animeId).episodePreviewUrls }, now)
            }
        }!!
        return entry.previews.await()
    }

    private fun Entry.isStaleEmpty(now: Long): Boolean =
        previews.isCompleted && now - createdAtMs > EMPTY_TTL_MS && previews.getCompleted().isEmpty()

    private fun String.withTvSize(): String = replace(MOBILE_SIZE_SEGMENT, TV_SIZE_SEGMENT)

    private companion object {
        const val EMPTY_TTL_MS = 60_000L
        const val MOBILE_SIZE_SEGMENT = "/w300/"
        const val TV_SIZE_SEGMENT = "/w780/"
    }
}
