package su.afk.yummy.tv.core.network.connectivity

import kotlinx.coroutines.flow.StateFlow

/** Текущее состояние подключения к интернету, без привязки к платформенному API. */
interface NetworkConnectivityMonitor {
    val isOnline: StateFlow<Boolean>

    /** Есть валидная безлимитная сеть (Wi-Fi, Ethernet): трафик по ней не тарифицируется. */
    val isUnmetered: StateFlow<Boolean>

    /**
     * Быстрая проба реальной доступности интернета. Флаг [isOnline] (`VALIDATED`) врёт при VPN,
     * captive portal и заблокированных проверках Google: связь есть, а система считает её
     * недоступной. Без сети отказывает сразу, с сетью укладывается в пару секунд.
     */
    suspend fun isInternetReachable(): Boolean
}
