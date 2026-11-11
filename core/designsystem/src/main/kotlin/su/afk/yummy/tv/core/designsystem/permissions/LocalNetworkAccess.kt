package su.afk.yummy.tv.core.designsystem.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Только `ACCESS_LOCAL_NETWORK` (без `NEARBY_WIFI_DEVICES`, нужного для NSD): им закрыты обычные сокетные
 * запросы к адресам локальной сети, например к TorrServer на `192.168.x.x`. Пусто, если платформа не знает
 * такого runtime-разрешения (Android до 16 и прошивки без Local Network Protection): тогда доступ не ограничен.
 */
private fun Context.requiredLocalNetworkAccessPermissions(): List<String> =
    requiredLocalNetworkPermissions().filter { it == Manifest.permission.ACCESS_LOCAL_NETWORK }

/** `true`, если запросам к локальной сети ничто не мешает. */
fun Context.hasLocalNetworkAccess(): Boolean = requiredLocalNetworkAccessPermissions().all {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

/**
 * Возвращает действие «запросить доступ к локальной сети»: показывает системный диалог, если разрешения
 * ещё нет, и ничего не делает, если оно есть. Без объясняющих диалогов: вызывается по явному действию
 * пользователя (включил торренты), поэтому причина понятна.
 */
@Composable
fun rememberLocalNetworkAccessRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    return remember(context, launcher) {
        {
            val permissions = context.requiredLocalNetworkAccessPermissions()
            if (permissions.isNotEmpty() && !context.hasLocalNetworkAccess()) {
                launcher.launch(permissions.toTypedArray())
            }
        }
    }
}
