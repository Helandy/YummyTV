package su.afk.yummy.tv.core.designsystem.skeleton

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `false`, если в системе включено «Убрать анимации» (масштаб длительности аниматоров = 0).
 *
 * Читается один раз на композицию: настройку меняют в системных параметрах, экран к этому
 * моменту обычно пересоздаётся.
 */
@Composable
internal fun rememberAnimationsEnabled(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    return remember(contentResolver) {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}
