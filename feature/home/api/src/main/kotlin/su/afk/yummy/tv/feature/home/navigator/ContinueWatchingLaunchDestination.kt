package su.afk.yummy.tv.feature.home.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.navigation.scene.FullscreenDestination

/**
 * Запуск «продолжить просмотр» тайтла [animeId] извне (ярлык, виджет): экран разрешает серию
 * и позицию и заменяет себя плеером. Полноэкранный, чтобы рейка не мелькала перед плеером.
 */
@Serializable
data class ContinueWatchingLaunchDestination(val animeId: Int) : NavKey, FullscreenDestination
