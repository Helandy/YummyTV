package su.afk.yummy.tv.core.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Следит за наличием валидного (провалидированного, не captive-portal) интернет-подключения
 * через [ConnectivityManager.registerNetworkCallback] на всё время жизни процесса — колбэк не
 * снимается, т.к. монитор живёт в [SingletonComponent] и переживает приложение целиком.
 */
@Singleton
class AndroidNetworkConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkConnectivityMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val validatedNetworks = mutableSetOf<Network>()
    private val unmeteredNetworks = mutableSetOf<Network>()

    override val isOnline = MutableStateFlow(connectivityManager.hasValidatedNetwork())

    override val isUnmetered = MutableStateFlow(connectivityManager.hasValidatedUnmeteredNetwork())

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        validatedNetworks.add(network)
                    } else {
                        validatedNetworks.remove(network)
                    }
                    if (capabilities.isValidatedUnmetered()) {
                        unmeteredNetworks.add(network)
                    } else {
                        unmeteredNetworks.remove(network)
                    }
                    isOnline.value = validatedNetworks.isNotEmpty()
                    isUnmetered.value = unmeteredNetworks.isNotEmpty()
                }

                override fun onLost(network: Network) {
                    validatedNetworks.remove(network)
                    unmeteredNetworks.remove(network)
                    isOnline.value = validatedNetworks.isNotEmpty()
                    isUnmetered.value = unmeteredNetworks.isNotEmpty()
                }
            })
    }

    override suspend fun isInternetReachable(): Boolean = withContext(Dispatchers.IO) {
        // По IP, без DNS: в сети без интернета отказывает сразу, а не ждёт резолвер.
        PROBE_ADDRESSES.any { (host, port) ->
            runCatching {
                Socket().use { it.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MS) }
            }.isSuccess
        }
    }

    private fun ConnectivityManager.hasValidatedNetwork(): Boolean {
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun ConnectivityManager.hasValidatedUnmeteredNetwork(): Boolean {
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.isValidatedUnmetered()
    }

    private fun NetworkCapabilities.isValidatedUnmetered(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

    private companion object {
        val PROBE_ADDRESSES = listOf("1.1.1.1" to 443, "8.8.8.8" to 443)
        const val PROBE_TIMEOUT_MS = 1_500
    }
}
