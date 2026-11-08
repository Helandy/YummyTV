// Part of YummyTV — https://github.com/Helandy/YummyTV (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.core.network.di

import javax.inject.Qualifier

/**
 * Ktor-клиент для получения потока плеера (HTML/m3u8 страниц плееров). Единственный, на котором
 * стоит `ResolveProgressPlugin`: запросы остальных клиентов не попадают в шаги и детали резолва.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerResolveClient
