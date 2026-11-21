package su.afk.yummy.tv.feature.player.common.utils

import androidx.annotation.OptIn
import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import kotlinx.coroutines.CompletableDeferred
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.model.PlayerPositionSnapshot
import kotlin.time.Duration.Companion.seconds

fun Player.positionSnapshot(fallbackDurationMs: Long): PlayerPositionSnapshot =
    PlayerPositionSnapshot(
        positionMs = currentPosition.coerceAtLeast(0L),
        durationMs = duration.takeIf { it > 0 } ?: fallbackDurationMs,
    )

/**
 * Тип ошибки плеера для аналитики. Имя класса самого исключения в release обфусцировано (`t15`) и
 * группировать по нему нечем, поэтому берём первое необфусцированное (JDK/Android) исключение из
 * цепочки причин, иначе общее «PlaybackException». Конкретный код — в `error_code`.
 */
fun PlaybackException.analyticsType(): String =
    generateSequence(cause) { it.cause }
        .take(CAUSE_SUMMARY_DEPTH)
        .map { it.javaClass }
        .firstOrNull { it.name.startsWith("java.") || it.name.startsWith("android.") }
        ?.simpleName
        ?.takeIf { it.isNotBlank() }
        ?: "PlaybackException"

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

/**
 * Плеер играет или вот-вот заиграет: воспроизведение запрошено, поток готов или догружается.
 * Пауза, ошибка (IDLE) и конец потока активным воспроизведением не считаются.
 */
fun Player.isPlaybackActive(): Boolean =
    playWhenReady && (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)

/**
 * Ждёт ближайшего события плеера (смена состояния, seek, загрузка и т.п.) без опроса.
 * Слушатель снимается в том же потоке, где его поставили (main), в том числе при отмене.
 */
suspend fun Player.awaitNextEvent() {
    val signal = CompletableDeferred<Unit>()
    val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            signal.complete(Unit)
        }
    }
    addListener(listener)
    try {
        signal.await()
    } finally {
        removeListener(listener)
    }
}

/** Спит, пока воспроизведение не станет активным ([isPlaybackActive]). */
suspend fun Player.awaitPlaybackActive() {
    while (!isPlaybackActive()) awaitNextEvent()
}
