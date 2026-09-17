package su.afk.yummy.tv.data.account.localauth

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.model.SessionTransferException
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.resume

/** Поиск ТВ в локальной сети по NSD. */
internal class NsdDeviceDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }

    fun devices(): Flow<List<DiscoveredDevice>> = callbackFlow {
        val found = LinkedHashMap<String, DiscoveredDevice>()
        // resolveService не выдерживает параллельных вызовов — резолвим строго по одному.
        val resolveLock = Mutex()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit

            override fun onServiceFound(service: NsdServiceInfo) {
                if (!LocalAuthContract.matchesServiceType(service.serviceType)) return
                launch {
                    val device = resolveLock.withLock {
                        withTimeoutOrNull(RESOLVE_TIMEOUT_MS) { resolve(service) }
                    } ?: return@launch
                    synchronized(found) { found[device.id] = device }
                    trySend(snapshot(found))
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                synchronized(found) { found -= service.serviceName }
                trySend(snapshot(found))
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(SessionTransferException("Discovery failed: $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { nsdManager.stopServiceDiscovery(this) }
            }
        }

        nsdManager.discoverServices(
            LocalAuthContract.SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            listener,
        )

        awaitClose { runCatching { nsdManager.stopServiceDiscovery(listener) } }
    }.flowOn(Dispatchers.IO)

    private fun snapshot(found: Map<String, DiscoveredDevice>): List<DiscoveredDevice> =
        synchronized(found) { found.values.toList() }

    private suspend fun resolve(service: NsdServiceInfo): DiscoveredDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            resolveViaCallback(service)
        } else {
            resolveViaLegacyListener(service)
        }

    private suspend fun resolveViaCallback(service: NsdServiceInfo): DiscoveredDevice? =
        suspendCancellableCoroutine { continuation ->
            val executor = Executors.newSingleThreadExecutor()
            lateinit var callback: NsdManager.ServiceInfoCallback
            callback = object : NsdManager.ServiceInfoCallback {
                private var delivered = false

                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                    if (!delivered) {
                        delivered = true
                        continuation.resume(null)
                    }
                    executor.shutdown()
                }

                override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                    if (delivered) return
                    delivered = true
                    continuation.resume(serviceInfo.toDiscoveredDevice())
                    runCatching { nsdManager.unregisterServiceInfoCallback(this) }
                }

                override fun onServiceLost() = Unit

                override fun onServiceInfoCallbackUnregistered() {
                    executor.shutdown()
                }
            }

            runCatching { nsdManager.registerServiceInfoCallback(service, executor, callback) }
                .onFailure {
                    executor.shutdown()
                    if (continuation.isActive) continuation.resume(null)
                }

            continuation.invokeOnCancellation {
                runCatching { nsdManager.unregisterServiceInfoCallback(callback) }
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun resolveViaLegacyListener(service: NsdServiceInfo): DiscoveredDevice? =
        suspendCancellableCoroutine { continuation ->
            nsdManager.resolveService(
                service,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        if (continuation.isActive) {
                            continuation.resume(serviceInfo.toDiscoveredDevice())
                        }
                    }
                },
            )
        }

    @SuppressLint("NewApi")
    private fun NsdServiceInfo.toDiscoveredDevice(): DiscoveredDevice? {
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hostAddresses.firstOrNull()?.hostAddress
        } else {
            @Suppress("DEPRECATION")
            host?.hostAddress
        }
        if (address.isNullOrBlank()) return null
        return DiscoveredDevice(id = serviceName, name = serviceName, host = address, port = port)
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MS = 10_000L
    }
}
