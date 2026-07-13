package su.afk.yummy.tv.core.designsystem.presenter.mobile

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import su.afk.yummy.tv.core.designsystem.R

private const val NOTIFICATION_PERMISSION_PREFERENCES = "notification_permission_gate"
private const val NOTIFICATION_PERMISSION_REQUESTED_KEY = "notification_permission_requested"

internal enum class NotificationPermissionDialog {
    Explanation,
    Reminder,
}

@Stable
class NotificationPermissionGateState internal constructor(
    private val needsPermission: () -> Boolean,
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
        action?.invoke()
    }
}

@Composable
fun rememberNotificationPermissionGate(): NotificationPermissionGateState {
    val context = LocalContext.current
    return remember(context) {
        NotificationPermissionGateState(
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

@Composable
fun NotificationPermissionGateHost(
    state: NotificationPermissionGateState,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val preferences = remember(context) {
        context.applicationContext.getSharedPreferences(
            NOTIFICATION_PERMISSION_PREFERENCES,
            Context.MODE_PRIVATE,
        )
    }
    var permissionWasRequested by remember(preferences) {
        mutableStateOf(preferences.getBoolean(NOTIFICATION_PERMISSION_REQUESTED_KEY, false))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        state.complete()
    }

    fun requestPermissionOrOpenSettings() {
        state.hideDialogForPermissionRequest()
        val canRequestAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } ?: true

        if (permissionWasRequested && !canRequestAgain) {
            try {
                context.openNotificationSettings()
            } finally {
                state.complete()
            }
        } else {
            permissionWasRequested = true
            preferences.edit()
                .putBoolean(NOTIFICATION_PERMISSION_REQUESTED_KEY, true)
                .apply()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    when (state.visibleDialog) {
        NotificationPermissionDialog.Explanation -> AlertDialog(
            onDismissRequest = state::complete,
            text = {
                Text(stringResource(R.string.notification_permission_explanation))
            },
            confirmButton = {
                TextButton(onClick = ::requestPermissionOrOpenSettings) {
                    Text(stringResource(R.string.notification_permission_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = state::showReminder) {
                    Text(stringResource(R.string.notification_permission_decline))
                }
            },
        )

        NotificationPermissionDialog.Reminder -> AlertDialog(
            onDismissRequest = state::complete,
            text = {
                Text(stringResource(R.string.notification_permission_settings_reminder))
            },
            confirmButton = {
                TextButton(onClick = ::requestPermissionOrOpenSettings) {
                    Text(stringResource(R.string.notification_permission_changed_mind))
                }
            },
            dismissButton = {
                TextButton(onClick = state::complete) {
                    Text(stringResource(R.string.notification_permission_ok))
                }
            },
        )

        null -> Unit
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openNotificationSettings() {
    val notificationSettingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    try {
        startActivity(notificationSettingsIntent)
    } catch (_: ActivityNotFoundException) {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            )
        )
    }
}
