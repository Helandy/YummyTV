package su.afk.yummy.tv.core.designsystem.skeleton

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Включён ли системный режим энергосбережения (Battery Saver); обновляется при переключении.
 *
 * Подписка на системный broadcast одна на процесс (на applicationContext), а не на каждый
 * композабл: флаг читают скелетоны в элементах списков.
 */
@Composable
fun rememberPowerSaveMode(): State<Boolean> {
    val context = LocalContext.current
    val flow = remember(context) { PowerSaveModeSource.observe(context) }
    return flow.collectAsState()
}

private object PowerSaveModeSource {
    @Volatile
    private var state: StateFlow<Boolean>? = null

    fun observe(context: Context): StateFlow<Boolean> =
        state ?: synchronized(this) { state ?: register(context.applicationContext).also { state = it } }

    private fun register(appContext: Context): StateFlow<Boolean> {
        val powerManager = appContext.getSystemService(PowerManager::class.java)
        val flow = MutableStateFlow(powerManager?.isPowerSaveMode == true)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                flow.value = powerManager?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        return flow.asStateFlow()
    }
}
