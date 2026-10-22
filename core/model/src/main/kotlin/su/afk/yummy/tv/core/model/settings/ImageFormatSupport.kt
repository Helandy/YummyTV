package su.afk.yummy.tv.core.model.settings

/** Фактическая проверка того, что устройство быстро и без сбоев декодирует современные форматы картинок. */
interface ImageFormatSupport {

    suspend fun isAvifSupported(): Boolean

    suspend fun isWebpSupported(): Boolean
}
