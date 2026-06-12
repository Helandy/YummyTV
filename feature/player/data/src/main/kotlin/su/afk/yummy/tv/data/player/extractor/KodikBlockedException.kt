package su.afk.yummy.tv.data.player.extractor

internal class KodikBlockedException(
    message: String?,
    val statusCode: Int?,
) : Exception(message)
