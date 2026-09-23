package su.afk.yummy.tv.core.designsystem.mobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal enum class NotificationPermissionDialog {
    Explanation,
    Reminder,
}

@Stable
class NotificationPermissionGateState internal constructor(
    private val needsPermission: () -> Boolean,
    private val runActionWhenDenied: Boolean,
) {
    private var pendingAction: (() -> Unit)? = null

    private var dialog by mutableStateOf<NotificationPermissionDialog?>(null)

    internal val visibleDialog: NotificationPermissionDialog?
        get() = dialog

    operator fun invoke(action: () -> Unit) {
        if (!needsPermission()) {
            action()
            return
        }
        if (pendingAction != null) return

        pendingAction = action
        dialog = NotificationPermissionDialog.Explanation
    }

    internal fun showReminder() {
        dialog = NotificationPermissionDialog.Reminder
    }

    internal fun hideDialogForPermissionRequest() {
        dialog = null
    }

    internal fun complete() {
        dialog = null
        val action = pendingAction
        pendingAction = null
        if (runActionWhenDenied || !needsPermission()) action?.invoke()
    }
}

/**
 * Есть ли разрешение на пуши. Пересчитывается на ON_RESUME (разрешение могли выдать или снять
 * в системных настройках, пока экран был в фоне) и при смене [refreshKey] — системный диалог
 * разрешения не всегда даёт ON_RESUME, поэтому после запроса ключ стоит менять вручную.
 */
@Composable
fun rememberNotificationPermissionGranted(refreshKey: Any? = Unit): Boolean {
    val context = LocalContext.current
    var granted by remember(context) { mutableStateOf(context.hasNotificationPermission()) }
    LaunchedEffect(context, refreshKey) { granted = context.hasNotificationPermission() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = context.hasNotificationPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

@Composable
fun rememberNotificationPermissionGate(
    runActionWhenDenied: Boolean = true,
): NotificationPermissionGateState {
    val context = LocalContext.current
    return remember(context, runActionWhenDenied) {
        NotificationPermissionGateState(
            runActionWhenDenied = runActionWhenDenied,
            needsPermission = { !context.hasNotificationPermission() }
        )
    }
}
