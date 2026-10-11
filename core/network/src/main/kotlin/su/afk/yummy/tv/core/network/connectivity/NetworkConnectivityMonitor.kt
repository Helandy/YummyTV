package su.afk.yummy.tv.core.network.connectivity

import kotlinx.coroutines.flow.StateFlow

/** Текущее состояние подключения к интернету, без привязки к платформенному API. */
interface NetworkConnectivityMonitor {
    val isOnline: StateFlow<Boolean>

    /** Есть валидная безлимитная сеть (Wi-Fi, Ethernet): трафик по ней не тарифицируется. */
    val isUnmetered: StateFlow<Boolean>
}
