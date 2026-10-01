package su.afk.yummy.tv.domain.update.model

/** Ошибка фоновой загрузки APK обновления, пересказанная по сообщению воркера. */
class UpdateDownloadException(message: String?) : Exception(message)
