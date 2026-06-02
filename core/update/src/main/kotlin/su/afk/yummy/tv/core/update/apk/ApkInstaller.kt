package su.afk.yummy.tv.core.update.apk

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.utils.PlatformCapabilities
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApkInstaller(private val context: Context) {

    suspend fun install(apkFile: File) = withContext(Dispatchers.IO) {
        if (!PlatformCapabilities.supportsInAppUpdateInstall) {
            error("Автоматическая установка обновлений недоступна на этой версии Android")
        }

        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            error("Разрешите установку приложений из этого источника и запустите обновление еще раз")
        }

        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)

        packageInstaller.openSession(sessionId).use { session ->
            session.openWrite("package", 0, apkFile.length()).use { output ->
                apkFile.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }

            session.commitAndAwaitResult(sessionId)
        }
    }

    private suspend fun PackageInstaller.Session.commitAndAwaitResult(sessionId: Int) {
        suspendCancellableCoroutine { continuation ->
            val action = "${context.packageName}.INSTALL_STATUS.$sessionId"
            val isReceiverRegistered = AtomicBoolean(true)

            fun unregisterReceiver(receiver: BroadcastReceiver) {
                if (isReceiverRegistered.compareAndSet(true, false)) {
                    runCatching { context.unregisterReceiver(receiver) }
                }
            }

            lateinit var receiver: BroadcastReceiver
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
                        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                            val confirmIntent = intent.confirmIntent()
                            if (confirmIntent == null) {
                                unregisterReceiver(this)
                                continuation.resumeWithException(IllegalStateException("Не удалось открыть подтверждение установки"))
                                return
                            }

                            confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(confirmIntent)
                        }

                        PackageInstaller.STATUS_SUCCESS -> {
                            unregisterReceiver(this)
                            continuation.resume(Unit)
                        }

                        else -> {
                            unregisterReceiver(this)
                            continuation.resumeWithException(IllegalStateException(intent.installErrorMessage(status)))
                        }
                    }
                }
            }

            registerReceiver(receiver, IntentFilter(action))
            continuation.invokeOnCancellation { unregisterReceiver(receiver) }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(action).setPackage(context.packageName),
                pendingIntentFlags(),
            )

            runCatching {
                commit(pendingIntent.intentSender)
            }.onFailure { e ->
                unregisterReceiver(receiver)
                continuation.resumeWithException(e)
            }
        }
    }

    private fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
    }

    private fun Intent.installErrorMessage(status: Int): String {
        val message = getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        if (!message.isNullOrBlank()) return message

        return when (status) {
            PackageInstaller.STATUS_FAILURE_ABORTED -> "Установка отменена"
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "Установка заблокирована системой"
            PackageInstaller.STATUS_FAILURE_CONFLICT -> "Конфликт с уже установленным пакетом"
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "APK несовместим с устройством"
            PackageInstaller.STATUS_FAILURE_INVALID -> "Некорректный APK-файл"
            PackageInstaller.STATUS_FAILURE_STORAGE -> "Недостаточно места для установки"
            else -> "Не удалось установить обновление"
        }
    }

    private fun Intent.confirmIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_INTENT)
        }

    private fun pendingIntentFlags(): Int {
        val base = PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            base or PendingIntent.FLAG_MUTABLE
        } else {
            base
        }
    }
}
