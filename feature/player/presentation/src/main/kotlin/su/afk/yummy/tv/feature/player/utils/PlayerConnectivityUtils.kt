package su.afk.yummy.tv.feature.player.utils

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor

/**
 * Ждёт появления сети не дольше [timeoutMs]. Возвращает `true`, если сеть есть (или появилась), и
 * `false` по таймауту: монитор может ошибаться (VPN, captive portal), поэтому вызывающий всё
 * равно делает попытку.
 */
internal suspend fun NetworkConnectivityMonitor.awaitOnline(timeoutMs: Long): Boolean {
    if (isOnline.value) return true
    return withTimeoutOrNull(timeoutMs) { isOnline.first { it } } == true
}
