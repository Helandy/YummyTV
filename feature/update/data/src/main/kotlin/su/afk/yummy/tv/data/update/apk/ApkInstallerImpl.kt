package su.afk.yummy.tv.data.update.apk

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.update.R
import su.afk.yummy.tv.domain.update.model.UpdateInstallCancelledException
import su.afk.yummy.tv.domain.update.model.UpdatePermissionRequiredException
import su.afk.yummy.tv.domain.update.repository.ApkInstallRepository
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "ApkInstaller"

private const val URI_GRANT_FLAGS =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

internal class ApkInstallerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ApkInstallRepository {

    override suspend fun install(apkFile: File) = withContext(Dispatchers.IO) {
        if (!canInstallPackages()) {
            runCatching {
                context.startActivity(unknownSourcesSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { Log.w(TAG, "Не удалось открыть настройки неизвестных источников", it) }
            throw UpdatePermissionRequiredException(
                context.getString(R.string.update_install_unknown_sources_required),
            )
        }

        val packageInstaller = context.packageManager.packageInstaller
        abandonStaleSessions(packageInstaller)
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)

        try {
            packageInstaller.openSession(sessionId).use { session ->
                session.openWrite("package", 0, apkFile.length()).use { output ->
                    apkFile.inputStream().use { it.copyTo(output) }
                    session.fsync(output)
                }

                session.commitAndAwaitResult(sessionId)
            }
        } catch (e: Throwable) {
            // Брошенная сессия на старых Android блокирует следующую установку.
            runCatching { packageInstaller.abandonSession(sessionId) }
            throw e
        }
    }

    /** Незавершённые сессии прошлых попыток (отмена, падение) не должны копиться. */
    private fun abandonStaleSessions(packageInstaller: PackageInstaller) {
        runCatching {
            packageInstaller.mySessions.forEach { info ->
                runCatching { packageInstaller.abandonSession(info.sessionId) }
            }
        }
    }

    /** С API 26 разрешение выдаётся приложению, до него — глобальный переключатель «Неизвестные источники». */
    private fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        }

    private fun unknownSourcesSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

    private suspend fun PackageInstaller.Session.commitAndAwaitResult(sessionId: Int) {
        suspendCancellableCoroutine { continuation ->
            val action = "${context.packageName}.INSTALL_STATUS.$sessionId"
            val isReceiverRegistered = AtomicBoolean(true)
            val isConfirmShown = AtomicBoolean(false)

            fun unregisterReceiver(receiver: BroadcastReceiver) {
                if (isReceiverRegistered.compareAndSet(true, false)) {
                    runCatching { context.unregisterReceiver(receiver) }
                }
            }

            lateinit var receiver: BroadcastReceiver
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
                    Log.d(TAG, "Статус установки=$status: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
                    when (status) {
                        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                            if (!isConfirmShown.compareAndSet(false, true)) return
                            val confirmIntent = intent.confirmIntent()?.takeIf { it.isSafeToLaunch() }
                            if (confirmIntent == null) {
                                unregisterReceiver(this)
                                continuation.resumeWithException(
                                    IllegalStateException(
                                        context.getString(R.string.update_install_confirmation_error),
                                    ),
                                )
                                return
                            }

                            // Intent.removeFlags появился только в API 26: на Android 7 он падал NoSuchMethodError.
                            confirmIntent.flags = (confirmIntent.flags or Intent.FLAG_ACTIVITY_NEW_TASK) and URI_GRANT_FLAGS.inv()
                            runCatching { context.startActivity(confirmIntent) }.onFailure { e ->
                                Log.w(TAG, "Не удалось открыть подтверждение установки", e)
                                unregisterReceiver(this)
                                continuation.resumeWithException(
                                    IllegalStateException(
                                        context.getString(R.string.update_install_confirmation_error),
                                    ),
                                )
                            }
                        }

                        PackageInstaller.STATUS_SUCCESS -> {
                            unregisterReceiver(this)
                            continuation.resume(Unit)
                        }

                        else -> {
                            unregisterReceiver(this)
                            val message = intent.installErrorMessage(status)
                            continuation.resumeWithException(
                                if (status == PackageInstaller.STATUS_FAILURE_ABORTED) {
                                    UpdateInstallCancelledException(message)
                                } else {
                                    IllegalStateException(message)
                                },
                            )
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
        // До API 33 флаг RECEIVER_NOT_EXPORTED не существует и обычный registerReceiver делает
        // приёмник доступным любому приложению; ContextCompat закрывает его собственным разрешением.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun Intent.installErrorMessage(status: Int): String {
        val message = getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        if (!message.isNullOrBlank()) return message

        return when (status) {
            PackageInstaller.STATUS_FAILURE_ABORTED -> context.getString(R.string.update_install_error_aborted)
            PackageInstaller.STATUS_FAILURE_BLOCKED -> context.getString(R.string.update_install_error_blocked)
            PackageInstaller.STATUS_FAILURE_CONFLICT -> context.getString(R.string.update_install_error_conflict)
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> context.getString(R.string.update_install_error_incompatible)
            PackageInstaller.STATUS_FAILURE_INVALID -> context.getString(R.string.update_install_error_invalid)
            PackageInstaller.STATUS_FAILURE_STORAGE -> context.getString(R.string.update_install_error_storage)
            else -> context.getString(R.string.update_install_error_unknown)
        }
    }

    /** Подтверждение установки открывает системный установщик — собственные компоненты приложения не допускаем. */
    private fun Intent.isSafeToLaunch(): Boolean =
        component?.packageName != context.packageName && `package` != context.packageName

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
