package su.afk.yummy.tv.feature.playersetup.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.navigation.scene.FullscreenDestination

/** Одноразовая первичная настройка плеера, которая открывается после первого выбора интерфейса. */
@Serializable
data object PlayerSetupDestination : NavKey, FullscreenDestination
