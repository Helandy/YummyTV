package su.afk.yummy.tv.feature.player.delegate

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackProgressHandler
import su.afk.yummy.tv.feature.player.host.PlayerStateHost
import javax.inject.Inject

/**
 * Уход с экрана плеера: навигация не ждёт сеть, прогресс сохраняется в [ioScope] (переживает
 * `viewModelScope`), дочерние экраны открываются поверх деталей.
 */
internal class PlayerNavigationDelegate @Inject constructor(
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val progress: PlayerPlaybackProgressHandler,
    @IoApplicationScope private val ioScope: CoroutineScope,
) {
    /** Идёт переход на экран поверх плеера — уход в фон в этот момент не должен уводить в детали. */
    var isNavigatingToChildScreen = false
        private set

    private var isLeaving = false

    fun back(host: PlayerStateHost, beforeLeave: () -> Unit) {
        if (isLeaving) return
        isLeaving = true
        navigateAndSaveProgress(host) {
            beforeLeave()
            nav.back()
        }
    }

    fun openDetails(host: PlayerStateHost) {
        val animeId = host.state.animeId
        if (animeId <= 0) return
        navigateAndSaveProgress(host) {
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
        navigateAndSaveProgress(host) {
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
        }
    }

    private fun openChild(host: PlayerStateHost, childDestination: (Int) -> NavKey) {
        val animeId = host.state.animeId
        if (animeId <= 0 || isNavigatingToChildScreen) return
        isNavigatingToChildScreen = true
        navigateAndSaveProgress(host) {
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

    /**
     * Снимок прогресса берётся до навигации (после неё состояние уже не наше), сама отправка
     * идёт в [ioScope] и не блокирует уход даже при плохой сети.
     */
    private fun navigateAndSaveProgress(host: PlayerStateHost, navigate: () -> Unit) {
        val request = progress.currentProgressSaveRequest(state = host.state)
        navigate()
        if (request == null) return
        ioScope.launch {
            runSuspendCatching { progress.saveProgress(request) }
        }
    }
}
