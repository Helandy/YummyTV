package su.afk.yummy.tv.source.kodik.data.extractor

internal class KodikBlockedException(
    message: String?,
    val statusCode: Int?,
) : Exception(message)
