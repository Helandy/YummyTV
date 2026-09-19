package su.afk.yummy.tv.data.account.localauth

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Анонс ТВ в локальной сети по NSD. */
internal class NsdAdvertiser @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }

    fun register(port: Int, onRegistered: () -> Unit, onFailed: () -> Unit): Registration {
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = onRegistered()
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = onFailed()
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = LocalAuthContract.deviceServiceName()
            serviceType = LocalAuthContract.SERVICE_TYPE
            setPort(port)
        }
        // Без разрешения на локальную сеть (Android 16+) вызов кидает SecurityException
        // синхронно, минуя onRegistrationFailed.
        runCatching { nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { onFailed() }

        return Registration(nsdManager, listener)
    }

    /** Снятие анонса идемпотентно: остановка сервера может прийти несколькими путями. */
    internal class Registration(
        private val nsdManager: NsdManager,
        private val listener: NsdManager.RegistrationListener,
    ) {
        @Volatile
        private var unregistered = false

        fun unregister() {
            synchronized(this) {
                if (unregistered) return
                unregistered = true
            }
            runCatching { nsdManager.unregisterService(listener) }
        }
    }
}
