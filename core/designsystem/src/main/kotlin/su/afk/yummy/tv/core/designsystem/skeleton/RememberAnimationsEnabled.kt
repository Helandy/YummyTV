package su.afk.yummy.tv.core.designsystem.skeleton

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `false`, если в системе включено «Убрать анимации» (масштаб длительности аниматоров = 0)
 * или режим энергосбережения: бесконечные анимации (скелетоны, сезонные украшения) в нём
 * не крутим.
 *
 * Масштаб аниматоров читается один раз на композицию: настройку меняют в системных параметрах,
 * экран к этому моменту обычно пересоздаётся. Энергосбережение отслеживается на лету.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    val durationScaleEnabled = remember(contentResolver) {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val powerSaveMode by rememberPowerSaveMode()
    return durationScaleEnabled && !powerSaveMode
}
