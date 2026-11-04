package su.afk.yummy.tv.feature.player.host

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Где сейчас живёт плеер: на ТВ — записью в общем back stack, на телефоне — отдельной Activity
 * ([PlayerActivityHost]). ViewModel и делегат навигации работают только через этот класс.
 */
@Singleton
class PlayerHostNavigation @Inject internal constructor(
    private val nav: INavigationManager,
) {
    @Volatile
    private var activityHost: PlayerActivityHost? = null

    fun attach(host: PlayerActivityHost) {
        activityHost = host
    }

    fun detach(host: PlayerActivityHost) {
        if (activityHost === host) activityHost = null
    }

    /** Плеер сейчас показан: в Activity или как верхний ключ стека. */
    val isPlayerCurrent: Boolean
        get() = activityHost != null || nav.backStack.lastOrNull() is PlayerDestination

    fun replacePlayer(destination: PlayerDestination) {
        val host = activityHost
        if (host != null) host.onPlayerDestinationChanged(destination) else nav.replace(destination)
    }

    fun back() {
        val host = activityHost
        if (host != null) host.finishPlayer() else nav.back()
    }

    fun openDetails(details: NavKey) {
        val host = activityHost
        if (host == null) {
            nav.navigate(details)
            return
        }
        if (nav.backStack.lastOrNull() != details) nav.navigate(details)
        host.finishPlayer()
    }

    fun openChild(details: NavKey, child: NavKey) {
        val host = activityHost
        if (host != null) {
            // Плеера в стеке нет: под ним лежит экран, с которого его открыли.
            if (nav.backStack.lastOrNull() != details) nav.navigate(details)
            nav.navigate(child)
            host.finishPlayer()
            return
        }
        val previous = nav.backStack.getOrNull(nav.backStack.lastIndex - 1)
        if (previous == details) {
            nav.replace(child)
        } else {
            nav.replace(details)
            nav.navigate(child)
        }
    }

    /** Только ТВ: плеер уходит со стека, когда приложение свернули. */
    fun returnToDetailsAfterTvBackground(details: NavKey?) {
        if (details == null) {
            nav.back()
            return
        }
        val previous = nav.backStack.getOrNull(nav.backStack.lastIndex - 1)
        if (previous == details) nav.back() else nav.replace(details)
    }
}
