package su.afk.yummy.tv.feature.player.common.utils

import androidx.annotation.OptIn
import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.model.PlayerPositionSnapshot
import kotlin.time.Duration.Companion.seconds

fun Player.positionSnapshot(fallbackDurationMs: Long): PlayerPositionSnapshot =
    PlayerPositionSnapshot(
        positionMs = currentPosition.coerceAtLeast(0L),
        durationMs = duration.takeIf { it > 0 } ?: fallbackDurationMs,
    )

fun PlaybackException.analyticsType(): String =
    this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

/**
 * Цепочка причин ошибки плеера для аналитики.
 *
 * Имена классов в release обфусцированы (`y14`), поэтому типы Media3 описываются литералами:
 * по ним видно, пришёл ли битый контейнер, нестандартный HTTP-ответ (код и Content-Type) или обрыв
 * чтения (позиция/длина запроса). Прочие исключения — по имени, если оно из JDK/Android.
 */
@OptIn(UnstableApi::class)
fun PlaybackException.causeSummary(): String? =
    generateSequence(cause) { it.cause }
        .take(CAUSE_SUMMARY_DEPTH)
        .joinToString(" <- ") { it.describeCause() }
        .takeIf { it.isNotBlank() }
        ?.take(CAUSE_SUMMARY_MAX_LENGTH)

/**
 * HTTP-код нестандартного ответа из цепочки причин, или null — ошибка пришла не от HTTP-источника.
 *
 * Та же глубина, что у [causeSummary], поэтому код и текст причины всегда описывают одну ошибку.
 */
@OptIn(UnstableApi::class)
fun PlaybackException.httpStatusCode(): Int? =
    generateSequence(cause) { it.cause }
        .take(CAUSE_SUMMARY_DEPTH)
        .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
        .firstOrNull()
        ?.responseCode

@OptIn(UnstableApi::class)
private fun Throwable.describeCause(): String = when (this) {
    is ParserException ->
        "ParserException(malformed=$contentIsMalformed, dataType=$dataType, msg=${message.orEmpty().take(PARSER_MESSAGE_LENGTH)})"

    is HttpDataSource.InvalidResponseCodeException -> {
        val contentType = headerFields.entries
            .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
            ?.value?.firstOrNull()
        "HttpStatus($responseCode, contentType=$contentType)"
    }

    is HttpDataSource.HttpDataSourceException ->
        "HttpDataSource(type=$type, position=${dataSpec.position}, length=${dataSpec.length})"

    else -> javaClass.name
        .takeIf { it.startsWith("java.") || it.startsWith("android.") }
        ?.substringAfterLast('.')
        ?: "other"
}

private const val CAUSE_SUMMARY_DEPTH = 4
private const val CAUSE_SUMMARY_MAX_LENGTH = 240
private const val PARSER_MESSAGE_LENGTH = 60

fun calculateBufferedProgress(
    bufferedPosition: Long,
    currentPosition: Long,
    duration: Long,
): Float {
    if (duration <= 0L) return 0f
    val playedProgress = currentPosition.toFloat() / duration
    val loadedProgress = bufferedPosition.coerceAtLeast(0L).toFloat() / duration
    return loadedProgress.coerceIn(playedProgress.coerceIn(0f, 1f), 1f)
}

/** Период polling-цикла позиции/буфера плеера — одинаковый для ТВ и мобилки. */
val PLAYER_PROGRESS_POLL_INTERVAL = 1.seconds

/** Обновляет долю загруженного буфера по плееру — общий шаг polling-циклов ТВ и мобилки. */
fun PlayerPlaybackProgressState.updateBufferedProgress(
    player: Player,
    currentPositionMs: Long = currentPosition,
    durationMs: Long = duration,
) {
    bufferedProgress = calculateBufferedProgress(
        bufferedPosition = player.bufferedPosition,
        currentPosition = currentPositionMs,
        duration = durationMs,
    )
}
