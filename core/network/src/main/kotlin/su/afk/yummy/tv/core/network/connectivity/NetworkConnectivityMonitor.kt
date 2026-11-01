package su.afk.yummy.tv.core.network.connectivity

import kotlinx.coroutines.flow.StateFlow

/** Текущее состояние подключения к интернету, без привязки к платформенному API. */
interface NetworkConnectivityMonitor {
    val isOnline: StateFlow<Boolean>

    /** Есть валидная безлимитная сеть (Wi-Fi, Ethernet): трафик по ней не тарифицируется. */
    val isUnmetered: StateFlow<Boolean>

    /**
     * Интернета точно нет: нет валидной сети и проба [isInternetReachable] не достучалась (или сетей
     * нет вовсе, например «Самолёт»). В отличие от `!isOnline`, не срабатывает при VPN и captive
     * portal, где связь есть, а флаг `VALIDATED` не выставлен. Для текстов ошибок и автоповтора.
     */
    val isOffline: StateFlow<Boolean>

    /**
     * Быстрая проба реальной доступности интернета. Флаг [isOnline] (`VALIDATED`) врёт при VPN,
     * captive portal и заблокированных проверках Google: связь есть, а система считает её
     * недоступной. Без сети отказывает сразу, с сетью укладывается в пару секунд.
     */
    suspend fun isInternetReachable(): Boolean
}
