package su.afk.yummy.tv.core.utils.kodik.di

import javax.inject.Qualifier

/** Ktor-клиент превью серий Kodik; провайдер лежит в `core/network` рядом с остальными клиентами. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KodikHttpClient
