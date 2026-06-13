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
import su.afk.yummy.tv.core.update.R
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApkInstaller(private val context: Context) {

    suspend fun install(apkFile: File) = withContext(Dispatchers.IO) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            error(context.getString(R.string.update_install_unknown_sources_required))
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
                                continuation.resumeWithException(
                                    IllegalStateException(
                                        context.getString(R.string.update_install_confirmation_error)
                                    )
                                )
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
            PackageInstaller.STATUS_FAILURE_ABORTED -> context.getString(R.string.update_install_error_aborted)
            PackageInstaller.STATUS_FAILURE_BLOCKED -> context.getString(R.string.update_install_error_blocked)
            PackageInstaller.STATUS_FAILURE_CONFLICT -> context.getString(R.string.update_install_error_conflict)
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> context.getString(R.string.update_install_error_incompatible)
            PackageInstaller.STATUS_FAILURE_INVALID -> context.getString(R.string.update_install_error_invalid)
            PackageInstaller.STATUS_FAILURE_STORAGE -> context.getString(R.string.update_install_error_storage)
            else -> context.getString(R.string.update_install_error_unknown)
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
