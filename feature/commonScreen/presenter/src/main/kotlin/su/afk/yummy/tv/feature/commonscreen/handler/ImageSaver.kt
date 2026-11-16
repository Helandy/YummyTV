package su.afk.yummy.tv.feature.commonscreen.handler

/** Сохраняет картинку по URL в галерею устройства. */
internal interface ImageSaver {

    /** Скачивает [url] и кладёт файл в галерею; сбой возвращается как [Result.failure]. */
    suspend fun save(url: String): Result<Unit>
}
