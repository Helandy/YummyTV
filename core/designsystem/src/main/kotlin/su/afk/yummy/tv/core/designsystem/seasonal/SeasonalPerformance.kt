package su.afk.yummy.tv.core.designsystem.seasonal

import android.app.ActivityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled

private const val MinOverlayTotalMemBytes = 1_610_612_736L // 1,5 ГБ

/**
 * Можно ли рисовать постоянные полноэкранные анимации (снег, мыши): не на устройствах с малой
 * памятью (системный флаг «low RAM» или меньше 1,5 ГБ оперативки) и не при системном
 * «Убрать анимации».
 */
@Composable
internal fun rememberSeasonalOverlayEnabled(): Boolean {
    val animationsEnabled = rememberAnimationsEnabled()
    val context = LocalContext.current
    val lowMemory = remember(context) {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val totalMem = ActivityManager.MemoryInfo()
            .also { activityManager?.getMemoryInfo(it) }
            .totalMem
        activityManager?.isLowRamDevice == true || (totalMem in 1 until MinOverlayTotalMemBytes)
    }
    return animationsEnabled && !lowMemory
}
