package su.afk.yummy.tv.core.designsystem.permissions

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Разрешения, без которых приложение не видит локальную подсеть.
 *
 * С Android 16 (API 36) работает Local Network Protection: NSD, mDNS и любые запросы к адресам
 * локальной сети требуют [Manifest.permission.ACCESS_LOCAL_NETWORK]. Без него `registerService`
 * молча уходит в `onRegistrationFailed`, а в logcat остаётся только `AppOps: Operation not found`.
 */
fun localNetworkPermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        add(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
}

fun Context.hasLocalNetworkPermissions(): Boolean = localNetworkPermissions().all { permission ->
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/** Что показывает экран поверх своего контента. Внешний вид диалогов — за платформенным UI. */
enum class LocalNetworkPermissionStep {
    /** Диалогов нет. */
    Idle,

    /** Объяснение перед системным запросом. */
    Rationale,

    /** Разрешение отклонено навсегда — остаётся только отправить в настройки приложения. */
    Blocked,
}

/**
 * Последовательность «объяснение → системный запрос → при отказе навсегда настройки».
 * Логика общая для мобильной и ТВ-сборки, сами диалоги каждая рисует по-своему.
 */
@Stable
class LocalNetworkPermissionGateState internal constructor(
    private val context: Context,
    private val onGranted: () -> Unit,
    private val onDenied: () -> Unit,
) {
    var step by mutableStateOf(LocalNetworkPermissionStep.Idle)
        private set

    internal var launchSystemRequest: () -> Unit = {}

    /** Точка входа: разрешения уже есть — идём дальше, иначе объясняем, зачем они. */
    fun start() {
        if (context.hasLocalNetworkPermissions()) {
            step = LocalNetworkPermissionStep.Idle
            onGranted()
        } else {
            step = LocalNetworkPermissionStep.Rationale
        }
    }

    /** Подтверждение в нашем диалоге: отсюда уже системный запрос. */
    fun confirm() {
        step = LocalNetworkPermissionStep.Idle
        launchSystemRequest()
    }

    /** Отказ в нашем диалоге: системный запрос не показываем вовсе. */
    fun dismiss() {
        step = LocalNetworkPermissionStep.Idle
        onDenied()
    }

    fun openSettings() {
        step = LocalNetworkPermissionStep.Idle
        context.openAppDetailsSettings()
        onDenied()
    }

    internal fun onSystemResult(granted: Boolean) {
        when {
            granted -> {
                step = LocalNetworkPermissionStep.Idle
                onGranted()
            }
            // Повторно спросить система уже не даст — зовём в настройки.
            context.isLocalNetworkPermissionBlocked() -> step = LocalNetworkPermissionStep.Blocked
            else -> {
                step = LocalNetworkPermissionStep.Idle
                onDenied()
            }
        }
    }
}

@Composable
fun rememberLocalNetworkPermissionGate(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): LocalNetworkPermissionGateState {
    val context = LocalContext.current
    val currentOnGranted by rememberUpdatedState(onGranted)
    val currentOnDenied by rememberUpdatedState(onDenied)
    val state = remember(context) {
        LocalNetworkPermissionGateState(
            context = context,
            onGranted = { currentOnGranted() },
            onDenied = { currentOnDenied() },
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        state.onSystemResult(result.values.all { it })
    }
    val permissions = remember { localNetworkPermissions().toTypedArray() }
    state.launchSystemRequest = {
        if (permissions.isEmpty()) state.onSystemResult(granted = true) else launcher.launch(permissions)
    }
    return state
}

private fun Context.isLocalNetworkPermissionBlocked(): Boolean {
    val activity = findActivity() ?: return false
    return localNetworkPermissions().any { permission ->
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openAppDetailsSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    runCatching { startActivity(intent) }
        .onFailure { if (it !is ActivityNotFoundException) throw it }
}
