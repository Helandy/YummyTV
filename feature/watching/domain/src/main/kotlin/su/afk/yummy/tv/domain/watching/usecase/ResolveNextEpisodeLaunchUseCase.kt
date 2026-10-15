package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.watching.ContinueWatchingLaunch
import su.afk.yummy.tv.domain.anime.usecase.FetchAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.RemoveCachedContinueWatchingUseCase
import su.afk.yummy.tv.domain.watching.utils.resolveNextEpisodeLaunch
import javax.inject.Inject

/**
 * Разрешает запуск карточки следующей серии после досмотренной на сервере: обновляет видео тайтла
 * и ищет серию по номеру, запуск — с начала. Возвращает null, если серии нет.
 *
 * Если серии нет в свежем списке с сервера (сериал досмотрен или серия ещё не вышла), тайтл
 * скрывается из «Продолжить просмотр» до новой активности — иначе карточка несуществующей серии
 * висела бы, пока запись не уйдёт из серверной истории. Без сети решает по кэшу и ничего не скрывает:
 * старый кэш мог не знать о новой серии.
 */
internal class ResolveNextEpisodeLaunchUseCase @Inject constructor(
    private val fetchAnimeVideos: FetchAnimeVideosUseCase,
    private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase,
    private val removeCachedContinueWatching: RemoveCachedContinueWatchingUseCase,
) {

    suspend operator fun invoke(entry: HomeContinueWatchingItem): ContinueWatchingLaunch? {
        val freshVideos = runSuspendCatching { fetchAnimeVideos(entry.animeId) }.getOrNull()
        if (freshVideos == null) {
            val cachedVideos = runSuspendCatching { getCachedAnimeVideos(entry.animeId) }.getOrNull()
            return cachedVideos?.let { resolveNextEpisodeLaunch(entry, it) }
        }
        val launch = resolveNextEpisodeLaunch(entry, freshVideos)
        if (launch == null && freshVideos.isNotEmpty()) {
            runSuspendCatching { removeCachedContinueWatching(entry.animeId) }
        }
        return launch
    }
}
