package su.afk.yummy.tv.core.network.di

import javax.inject.Qualifier

/**
 * OkHttp-клиент воспроизведения (сегменты и плейлисты): производный от базового, свой диспетчер, таймауты [NetworkTimeouts.Streaming].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StreamingOkHttp
