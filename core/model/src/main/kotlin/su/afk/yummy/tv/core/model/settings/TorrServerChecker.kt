package su.afk.yummy.tv.core.model.settings

/**
 * Проверка связи с TorrServer по настройкам. Реализует необязательный торрент-модуль: экран настроек получает её
 * через `Optional`, и без модуля кнопка проверки не показывается.
 */
interface TorrServerChecker {
    suspend fun check(settings: TorrentSettings): TorrServerCheckResult
}
