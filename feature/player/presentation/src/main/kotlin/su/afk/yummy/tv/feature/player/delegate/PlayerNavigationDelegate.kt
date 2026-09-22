package su.afk.yummy.tv.feature.player.delegate

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackProgressHandler
import su.afk.yummy.tv.feature.player.host.PlayerStateHost
import javax.inject.Inject

/** Уход с экрана плеера: прогресс сохраняется до навигации, дочерние экраны открываются поверх деталей. */
internal class PlayerNavigationDelegate @Inject constructor(
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val progress: PlayerPlaybackProgressHandler,
    @IoApplicationScope private val ioScope: CoroutineScope,
) {
    /** Идёт переход на экран поверх плеера — уход в фон в этот момент не должен уводить в детали. */
    var isNavigatingToChildScreen = false
        private set

    fun back(host: PlayerStateHost, beforeLeave: () -> Unit) {
        saveProgressThenNavigate(host) {
            beforeLeave()
            nav.back()
        }
    }

    fun openDetails(host: PlayerStateHost) {
        val animeId = host.state.animeId
        if (animeId <= 0) return
        saveProgressThenNavigate(host) {
            nav.navigate(detailsNavigator.getDetailsDest(animeId))
        }
    }

    fun openRating(host: PlayerStateHost) {
        openChild(host, detailsNavigator::getRatingDest)
    }

    fun openSubscriptions(host: PlayerStateHost) {
        openChild(host, detailsNavigator::getSubscriptionsDest)
    }

    /**
     * Убирает плеер со стека, когда приложение уходит в фон.
     *
     * Навигация выполняется синхронно, прямо в обработке `ON_STOP`: `onSaveInstanceState`
     * вызывается сразу за `onStop`, и отложенная в корутину замена ключа не успевает попасть
     * в сохранённый back stack — после смерти процесса приложение восстанавливалось в плеере
     * на той серии, с которой его когда-то открыли. Сохранение прогресса уезжает в
     * [ioScope]: `nav.replace` уничтожает NavEntry плеера, и `viewModelScope` вместе с ним.
     */
    fun returnToDetailsAfterTvBackground(host: PlayerStateHost, beforeLeave: () -> Unit) {
        if (isNavigatingToChildScreen) return
        val animeId = host.state.animeId
        val request = progress.currentProgressSaveRequest(state = host.state)

        beforeLeave()
        if (animeId <= 0) {
            nav.back()
        } else {
            val detailsDestination = detailsNavigator.getDetailsDest(animeId)
            val previousDestination = nav.backStack.getOrNull(nav.backStack.lastIndex - 1)
            if (previousDestination == detailsDestination) {
                nav.back()
            } else {
                nav.replace(detailsDestination)
            }
        }

        if (request == null) return
        ioScope.launch {
            runCatching { progress.saveProgress(request) }
        }
    }

    private fun openChild(host: PlayerStateHost, childDestination: (Int) -> NavKey) {
        val animeId = host.state.animeId
        if (animeId <= 0 || isNavigatingToChildScreen) return
        isNavigatingToChildScreen = true
        saveProgressThenNavigate(host) {
            navigateFromPlayerToChild(animeId, childDestination(animeId))
        }
    }

    private fun navigateFromPlayerToChild(animeId: Int, childDestination: NavKey) {
        val detailsDestination = detailsNavigator.getDetailsDest(animeId)
        val previousDestination = nav.backStack.getOrNull(nav.backStack.lastIndex - 1)
        if (previousDestination == detailsDestination) {
            nav.replace(childDestination)
        } else {
            nav.replace(detailsDestination)
            nav.navigate(childDestination)
        }
    }

    private fun saveProgressThenNavigate(host: PlayerStateHost, navigate: () -> Unit) {
        val request = progress.currentProgressSaveRequest(state = host.state)
        if (request == null) {
            navigate()
            return
        }

        host.scope.launch {
            runCatching {
                progress.saveProgress(request)
            }
            navigate()
        }
    }
}
