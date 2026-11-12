package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerState

/**
 * Короткая сводка ошибки воспроизведения для аналитики: код плеера, HTTP-статус CDN и причина.
 * Например `ERROR_CODE_IO_BAD_HTTP_STATUS http=403 cause=…`.
 */
internal fun PlayerState.Event.PlaybackError.recoveryFailureSummary(): String =
    listOfNotNull(
        errorCode,
        httpStatusCode?.let { "http=$it" },
        cause?.let { "cause=$it" },
    ).joinToString(" ").ifEmpty { message }
