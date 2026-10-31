package su.afk.yummy.tv.data.videodownload.worker

/**
 * Штатный отказ экспорта (папка недоступна, нет места): показываем пользователю, но не шлём как
 * error в крэш-аналитику.
 */
internal class ExpectedExportException(message: String) : IllegalStateException(message)
