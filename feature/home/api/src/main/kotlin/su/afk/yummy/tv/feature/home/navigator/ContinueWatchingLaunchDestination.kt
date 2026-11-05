package su.afk.yummy.tv.feature.home.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.navigation.scene.FullscreenDestination

/**
 * Запуск «продолжить просмотр» тайтла [animeId] извне (ярлык, виджет): экран разрешает серию
 * и позицию и заменяет себя плеером. С явным запросом ([isExplicit]) серия, балансер, озвучка и
 * позиция берутся из него, а не из записи просмотра. Полноэкранный, чтобы рейка не мелькала перед плеером.
 */
@Serializable
data class ContinueWatchingLaunchDestination(
    val animeId: Int,
    /** Явный запрос (диплинк `yummytv://play/...`): серия, балансер, озвучка и позиция, секунды. */
    val episode: String? = null,
    val player: String? = null,
    val dubbing: String? = null,
    val positionSeconds: Long? = null,
) : NavKey,
    FullscreenDestination {

    /** Задано ли хоть одно условие явного запроса; иначе запуск идёт по записи просмотра. */
    val isExplicit: Boolean
        get() = episode != null || player != null || dubbing != null || positionSeconds != null
}
