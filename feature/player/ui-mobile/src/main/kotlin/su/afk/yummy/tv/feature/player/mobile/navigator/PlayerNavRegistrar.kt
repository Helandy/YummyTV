package su.afk.yummy.tv.feature.player.mobile.navigator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.player.IMobilePlayerEntry
import su.afk.yummy.tv.feature.player.mobile.activity.PlayerActivity
import su.afk.yummy.tv.feature.player.navigator.PLAYER_CONTENT_KEY
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import javax.inject.Inject

/**
 * Мобильный плеер живёт в [PlayerActivity], а не в back stack: иначе при PiP от приложения
 * не осталось бы ничего, кроме маленького окна. Запись лишь запускает Activity и сразу снимает
 * ключ плеера со стека, чтобы все вызывающие по-прежнему открывали плеер через `nav.navigate`.
 */
class PlayerNavRegistrar @Inject constructor() : IMobilePlayerEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<PlayerDestination>(clazzContentKey = { PLAYER_CONTENT_KEY }) { dest ->
                val context = LocalContext.current
                LaunchedEffect(dest) {
                    PlayerActivity.start(context, dest)
                    if (nav.backStack.lastOrNull() is PlayerDestination) nav.back()
                }
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
        }
}
