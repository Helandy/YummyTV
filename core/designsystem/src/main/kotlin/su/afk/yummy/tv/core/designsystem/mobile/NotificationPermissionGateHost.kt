package su.afk.yummy.tv.core.designsystem.mobile

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.locals.LocalMarkNotificationPermissionRequested
import su.afk.yummy.tv.core.designsystem.locals.LocalNotificationPermissionRequested

@Composable
fun NotificationPermissionGateHost(
    state: NotificationPermissionGateState,
    @StringRes explanationRes: Int = R.string.notification_permission_explanation,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val permissionWasRequested by LocalNotificationPermissionRequested.current
        .collectAsStateWithLifecycle(initialValue = false)
    val markNotificationPermissionRequested = LocalMarkNotificationPermissionRequested.current
    val coroutineScope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        state.complete()
    }

    fun requestPermissionOrOpenSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
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
            coroutineScope.launch { markNotificationPermissionRequested() }
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    when (state.visibleDialog) {
        NotificationPermissionDialog.Explanation -> AlertDialog(
            onDismissRequest = state::complete,
            text = {
                Text(stringResource(explanationRes))
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
            ),
        )
    }
}
