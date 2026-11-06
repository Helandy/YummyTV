package su.afk.yummy.tv.core.network.di

import javax.inject.Qualifier

/**
 * OkHttp-клиент загрузок серий: производный от базового, свой диспетчер, таймауты [NetworkTimeouts.Download].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadOkHttp
