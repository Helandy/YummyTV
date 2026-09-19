package su.afk.yummy.tv.data.account.di

import javax.inject.Qualifier

/**
 * Клиент для передачи сессии на ТВ внутри локальной сети.
 *
 * Отдельный от общего не ради настроек, а ради движка: общий клиент собран на OkHttp, который
 * сверяется с `network_security_config` — а там cleartext разрешён только для localhost, и запрос
 * на `http://192.168.x.x` не доходит до сокета. CIO работает на сырых сокетах и этой проверки не
 * делает. Открывать cleartext всему приложению ради одного локального обмена не хочется, тем более
 * что сама сессия шифруется PIN-ом в [su.afk.yummy.tv.data.account.utils.LocalAuthCrypto].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class LocalAuthHttpClient
