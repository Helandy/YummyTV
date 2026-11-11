package su.afk.yummy.tv.core.network.di

import javax.inject.Qualifier

/**
 * Ktor-клиент торрент-источника: поиск раздач (JacRed/Jackett) и управление TorrServer по адресам, которые
 * задаёт пользователь (в том числе `http://` в LAN). JSON, без токена приложения и без логов тела: в запросах
 * бывает API-ключ Jackett. Сам видеопоток с TorrServer играет Media3, а не этот клиент.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TorrentHttpClient
