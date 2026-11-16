package su.afk.yummy.tv.core.designsystem.focus

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay

private val ConfirmKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/** Пауза без событий OK, после которой считаем, что клавишу уже отпустили. */
private const val RELEASE_IDLE_MS = 400L
private const val IDLE_CHECK_STEP_MS = 50L

/**
 * Для диалога, который открывается по удержанию OK: гасит «хвост» этого удержания, чтобы он не
 * прокликал сфокусированную внутри кнопку.
 *
 * Compose нажимает кнопку по KeyUp, а пока OK зажат, пульт шлёт повторные KeyDown — они доходят
 * до диалога, и отпускание превращается в клик. Поэтому события OK глотаются, пока они приходят:
 * охрана снимается по KeyUp либо когда события OK не приходят дольше [RELEASE_IDLE_MS] (клавишу
 * отпустили до появления диалога или пульт не шлёт повторов). Фиксированного окна от момента
 * появления диалога нет — при долгом удержании оно истекало бы раньше, чем отпустят кнопку.
 */
@Composable
fun Modifier.consumeLongPressRelease(): Modifier {
    var active by remember { mutableStateOf(true) }
    var lastConfirmEventMs by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }

    LaunchedEffect(Unit) {
        while (active) {
            delay(IDLE_CHECK_STEP_MS)
            if (SystemClock.uptimeMillis() - lastConfirmEventMs > RELEASE_IDLE_MS) active = false
        }
    }

    return onPreviewKeyEvent { event ->
        if (!active || event.key !in ConfirmKeys) return@onPreviewKeyEvent false
        lastConfirmEventMs = SystemClock.uptimeMillis()
        if (event.type == KeyEventType.KeyUp) active = false
        true
    }
}
