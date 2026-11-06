package su.afk.yummy.tv.core.network.di

import javax.inject.Qualifier

/**
 * OkHttp-клиент Alloha (страница плеера, `bnsi`, отчёты, WebSocket): производный от базового, свой диспетчер, таймауты [NetworkTimeouts.Alloha].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AllohaOkHttp
