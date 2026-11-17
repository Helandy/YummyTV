package su.afk.yummy.tv.core.model.settings

/**
 * Проверка связи с TorrServer и индексаторами раздач по настройкам. Реализует необязательный торрент-модуль:
 * экран настроек получает её через `Optional`, и без модуля кнопка проверки не показывается.
 */
interface TorrServerChecker {
    suspend fun check(settings: TorrentSettings): TorrServerCheckResult

    /** Доступность одного индексатора по адресу (и ключу для Jackett) из [settings]. */
    suspend fun checkIndexer(settings: TorrentSettings, indexer: TorrentIndexer): TorrentIndexerCheckResult
}
