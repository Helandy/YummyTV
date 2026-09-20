package su.afk.yummy.tv.core.designsystem.permissions

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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

/**
 * Из [localNetworkPermissions] — только те, что конкретная прошивка действительно знает как
 * runtime-разрешение.
 *
 * В Android 16 `ACCESS_LOCAL_NETWORK` объявлено за фиче-флагом Local Network Protection. На
 * прошивках без него (встречалось на Nothing OS) платформа такого разрешения не знает вовсе:
 * `checkSelfPermission` всегда отвечает отказом, системный диалог по нему не показывается,
 * тумблера в настройках приложения нет. Требовать такое разрешение — значит навсегда закрыть
 * пользователю экран, поэтому спрашиваем только то, что реально можно выдать.
 */
fun Context.requiredLocalNetworkPermissions(): List<String> =
    localNetworkPermissions().filter { packageManager.isRuntimePermission(it) }

fun Context.hasLocalNetworkPermissions(): Boolean =
    requiredLocalNetworkPermissions().all { it.isPermissionGranted(this) }

/** Недостающие разрешения короткими именами — для аналитики и логов, без пакета. */
fun Context.missingLocalNetworkPermissionNames(): String = requiredLocalNetworkPermissions()
    .filterNot { it.isPermissionGranted(this) }
    .joinToString(separator = ",") { it.substringAfterLast('.').lowercase() }

private fun PackageManager.isRuntimePermission(permission: String): Boolean =
    runCatching { getPermissionInfo(permission, 0) }
        .map { it.isDangerous() }
        .getOrDefault(false)

@Suppress("DEPRECATION")
private fun PermissionInfo.isDangerous(): Boolean {
    val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        protection
    } else {
        protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
    }
    return level == PermissionInfo.PROTECTION_DANGEROUS
}

private fun String.isPermissionGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, this) == PackageManager.PERMISSION_GRANTED

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

    /** Последний исход, о котором уже сообщили наружу. `null` — ещё ни разу не спрашивали. */
    private var lastResultGranted: Boolean? = null

    /** Ушли в системные настройки: итог станет известен только после возврата в приложение. */
    private var awaitingSettingsReturn = false

    /** Точка входа: разрешения уже есть — идём дальше, иначе объясняем, зачем они. */
    fun start() {
        if (context.hasLocalNetworkPermissions()) {
            step = LocalNetworkPermissionStep.Idle
            notifyGranted()
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
        notifyDenied()
    }

    /**
     * Уводим в настройки приложения и ничего не решаем за пользователя: выдал он разрешение или
     * нет, станет видно в [recheck] при возврате в приложение.
     */
    fun openSettings() {
        step = LocalNetworkPermissionStep.Idle
        awaitingSettingsReturn = true
        context.openAppDetailsSettings()
    }

    /**
     * Перечитывание состояния при возврате в приложение (ON_RESUME).
     *
     * Разрешение могли выдать где угодно — в настройках приложения или в шторке, — и без этой
     * проверки экран так и остался бы с прежним отказом. Первый вход на экран не трогаем: там
     * решение принимает [start], иначе поиск стартовал бы дважды.
     */
    fun recheck() {
        if (step == LocalNetworkPermissionStep.Rationale) return
        val returnedFromSettings = awaitingSettingsReturn
        awaitingSettingsReturn = false
        val granted = context.hasLocalNetworkPermissions()
        when {
            granted && (lastResultGranted == false || returnedFromSettings) -> {
                step = LocalNetworkPermissionStep.Idle
                notifyGranted()
            }

            !granted && returnedFromSettings -> {
                step = LocalNetworkPermissionStep.Idle
                notifyDenied()
            }
        }
    }

    internal fun onSystemResult(granted: Boolean) {
        when {
            granted -> {
                step = LocalNetworkPermissionStep.Idle
                notifyGranted()
            }
            // Повторно спросить система уже не даст — зовём в настройки.
            context.isLocalNetworkPermissionBlocked() -> {
                lastResultGranted = false
                step = LocalNetworkPermissionStep.Blocked
            }

            else -> {
                step = LocalNetworkPermissionStep.Idle
                notifyDenied()
            }
        }
    }

    private fun notifyGranted() {
        lastResultGranted = true
        onGranted()
    }

    private fun notifyDenied() {
        lastResultGranted = false
        onDenied()
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
    ) {
        // Карте результата не верим: прошивки возвращают в ней и разрешения, которых не давали.
        state.onSystemResult(context.hasLocalNetworkPermissions())
    }
    val permissions = remember(context) { context.requiredLocalNetworkPermissions().toTypedArray() }
    state.launchSystemRequest = {
        if (permissions.isEmpty()) state.onSystemResult(granted = true) else launcher.launch(permissions)
    }

    // Разрешение могли выдать в системных настройках — заметить это можно только на возврате.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.recheck()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

private fun Context.isLocalNetworkPermissionBlocked(): Boolean {
    val activity = findActivity() ?: return false
    return requiredLocalNetworkPermissions()
        .filterNot { it.isPermissionGranted(this) }
        .any { permission ->
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
