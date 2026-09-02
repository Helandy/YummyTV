package su.afk.yummy.tv.core.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
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

    override val isOnline = MutableStateFlow(connectivityManager.hasValidatedNetwork())

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
                    isOnline.value = validatedNetworks.isNotEmpty()
                }

                override fun onLost(network: Network) {
                    validatedNetworks.remove(network)
                    isOnline.value = validatedNetworks.isNotEmpty()
                }
            })
    }

    private fun ConnectivityManager.hasValidatedNetwork(): Boolean {
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
