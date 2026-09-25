package su.afk.yummy.tv.feature.account.mobile.localauth.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Сканер QR из Google Play Services: камеру и разрешение на неё берёт на себя системный экран
 * сканера, приложению CAMERA не нужен.
 *
 * Модуль сканера не качается при установке приложения — только когда открыт экран «Войти на ТВ»
 * (фоновая предзагрузка) или, если не успел, по нажатию на скан: тогда [isPreparing] показывает
 * загрузку, а скан стартует сам по её окончании.
 */
@Stable
internal class LocalAuthQrScannerState(
    private val scanner: GmsBarcodeScanner,
    private val moduleInstall: ModuleInstallClient,
    // State, а не лямбды: remember создаёт холдер один раз, а коллбэки экрана меняются. Значение
    // читается только внутри Task-слушателей, вне композиции — перекомпозиций это не вызывает.
    private val onScanned: State<(String) -> Unit>,
    private val onFailed: State<() -> Unit>,
) {
    var isPreparing by mutableStateOf(false)
        private set

    private var installListener: InstallStatusListener? = null

    /** Ответы Play Services приходят асинхронно — после ухода с экрана сканер открываться не должен. */
    private var isDisposed = false

    /** Фоновая загрузка модуля; если он уже есть, Play Services ничего не делают. */
    fun prefetch() {
        moduleInstall.installModules(ModuleInstallRequest.newBuilder().addApi(scanner).build())
    }

    fun scan() {
        if (isPreparing || isDisposed) return
        moduleInstall.areModulesAvailable(scanner)
            .addOnSuccessListener { response ->
                if (response.areModulesAvailable()) startScan() else installThenScan()
            }
            .addOnFailureListener { onFailed.value() }
    }

    fun dispose() {
        isDisposed = true
        unregisterInstallListener()
    }

    private fun unregisterInstallListener() {
        installListener?.let(moduleInstall::unregisterListener)
        installListener = null
    }

    private fun installThenScan() {
        isPreparing = true
        val listener = InstallStatusListener { update ->
            when (update.installState) {
                InstallState.STATE_COMPLETED -> finishInstall(success = true)
                InstallState.STATE_FAILED, InstallState.STATE_CANCELED -> finishInstall(success = false)
                else -> Unit
            }
        }
        installListener = listener
        val request = ModuleInstallRequest.newBuilder()
            .addApi(scanner)
            .setListener(listener)
            .build()
        moduleInstall.installModules(request)
            .addOnSuccessListener { response ->
                // Модуль мог докачаться между проверкой и запросом — тогда статусов не будет.
                if (response.areModulesAlreadyInstalled()) finishInstall(success = true)
            }
            .addOnFailureListener { finishInstall(success = false) }
    }

    private fun finishInstall(success: Boolean) {
        if (installListener == null) return
        unregisterInstallListener()
        isPreparing = false
        if (success) startScan() else onFailed.value()
    }

    private fun startScan() {
        if (isDisposed) return
        scanner.startScan()
            // rawValue бывает null у бинарных QR: пустая строка дойдёт до разбора как чужой QR.
            .addOnSuccessListener { barcode -> onScanned.value(barcode.rawValue.orEmpty()) }
            // Отмена скана пользователем сюда не приходит — у неё свой listener, и она молчит.
            .addOnFailureListener { onFailed.value() }
    }
}

/**
 * @param onFailed сканер не запустился: нет Play Services или модуль не скачался.
 */
@Composable
internal fun rememberLocalAuthQrScanner(
    onScanned: (String) -> Unit,
    onFailed: () -> Unit,
): LocalAuthQrScannerState {
    val context = LocalContext.current
    val currentOnScanned = rememberUpdatedState(onScanned)
    val currentOnFailed = rememberUpdatedState(onFailed)

    val state = remember(context) {
        LocalAuthQrScannerState(
            scanner = context.qrScanner(),
            moduleInstall = ModuleInstall.getClient(context),
            onScanned = currentOnScanned,
            onFailed = currentOnFailed,
        )
    }

    LaunchedEffect(state) { state.prefetch() }
    DisposableEffect(state) { onDispose { state.dispose() } }

    return state
}

private fun Context.qrScanner(): GmsBarcodeScanner = GmsBarcodeScanning.getClient(
    this,
    GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build(),
)
