package su.afk.yummy.tv.core.designsystem.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

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

@Composable
fun rememberNotificationPermissionGate(
    runActionWhenDenied: Boolean = true,
): NotificationPermissionGateState {
    val context = LocalContext.current
    return remember(context, runActionWhenDenied) {
        NotificationPermissionGateState(
            runActionWhenDenied = runActionWhenDenied,
            needsPermission = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
            }
        )
    }
}
