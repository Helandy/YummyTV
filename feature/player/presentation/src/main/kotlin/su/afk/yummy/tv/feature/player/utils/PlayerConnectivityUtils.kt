package su.afk.yummy.tv.feature.player.utils

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor

/**
 * Ждёт появления сети не дольше [timeoutMs]. Возвращает `true`, если сеть есть (или появилась), и
 * `false` по таймауту: вызывающий всё равно делает попытку.
 *
 * Флаг `isOnline` врёт при VPN и captive portal (связь есть, система считает её недоступной), поэтому
 * перед ожиданием делается быстрая проба [NetworkConnectivityMonitor.isInternetReachable]:
 * достучались до интернета — ждать нечего.
 */
internal suspend fun NetworkConnectivityMonitor.awaitOnline(timeoutMs: Long): Boolean {
    if (isOnline.value || isInternetReachable()) return true
    return withTimeoutOrNull(timeoutMs) { isOnline.first { it } } == true
}
