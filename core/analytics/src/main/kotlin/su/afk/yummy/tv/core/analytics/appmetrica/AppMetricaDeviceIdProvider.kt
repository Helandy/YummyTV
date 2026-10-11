package su.afk.yummy.tv.core.analytics.appmetrica

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.StartupParamsCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import su.afk.yummy.tv.core.analytics.api.AnalyticsDeviceIdProvider
import javax.inject.Inject
import kotlin.coroutines.resume

internal class AppMetricaDeviceIdProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val activationState: AppMetricaActivationState,
) : AnalyticsDeviceIdProvider {

    override suspend fun deviceId(): String? {
        // Пустой ключ — AppMetrica не активирована, ID не существует.
        if (!activationState.isActivated) return null
        return try {
            requestDeviceIdHash() ?: AppMetrica.getDeviceId(context)?.takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    /** Десятичный хэш DeviceID — именно он показывается в интерфейсе AppMetrica. */
    private suspend fun requestDeviceIdHash(): String? = suspendCancellableCoroutine { continuation ->
        val callback = object : StartupParamsCallback {
            override fun onReceive(result: StartupParamsCallback.Result?) {
                if (continuation.isActive) continuation.resume(result?.deviceIdHash?.takeIf { it.isNotBlank() })
            }

            override fun onRequestError(
                reason: StartupParamsCallback.Reason,
                result: StartupParamsCallback.Result?,
            ) {
                if (continuation.isActive) continuation.resume(result?.deviceIdHash?.takeIf { it.isNotBlank() })
            }
        }
        AppMetrica.requestStartupParams(
            context,
            callback,
            listOf(StartupParamsCallback.APPMETRICA_DEVICE_ID_HASH),
        )
    }
}
