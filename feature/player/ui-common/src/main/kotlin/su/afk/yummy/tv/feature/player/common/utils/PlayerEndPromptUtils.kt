package su.afk.yummy.tv.feature.player.common.utils

import su.afk.yummy.tv.feature.player.common.model.PlayerEndPromptState

val PlayerEndPromptState.isVisible: Boolean
    get() = this !is PlayerEndPromptState.Hidden

fun playerEndPromptFor(
    autoPlayNextEpisode: Boolean,
    delaySeconds: Int = PLAYER_END_PROMPT_COUNTDOWN_SECONDS,
): PlayerEndPromptState =
    if (autoPlayNextEpisode) {
        PlayerEndPromptState.WithCountdown(delaySeconds)
    } else {
        PlayerEndPromptState.WithoutCountdown
    }

/** ON_PAUSE: активный отсчёт вырождается в промпт без отсчёта. */
fun PlayerEndPromptState.downgradedCountdown(): PlayerEndPromptState =
    if (this is PlayerEndPromptState.WithCountdown) {
        PlayerEndPromptState.WithoutCountdown
    } else {
        this
    }

/** Часть потоков останавливается чуть раньше duration — считаем это концом эпизода. */
fun isAtPlayerEnd(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0L && durationMs - positionMs <= PLAYER_END_POSITION_TOLERANCE_MS

const val PLAYER_END_PROMPT_COUNTDOWN_SECONDS = 10

const val PLAYER_END_POSITION_TOLERANCE_MS = 500L

/** Ошибка источника в хвосте потока всё ещё считается концом серии, если до duration не больше этого. */
const val PLAYER_END_ERROR_TOLERANCE_MS = 2_000L

// Битый или обрезанный последний сегмент (Kodik отдаёт хвост в пару КБ, иногда с зависшим редиректом) и
// отказ CDN на нём: плеер дошёл до конца, но не смог дочитать контейнер.
private val END_OF_STREAM_ERROR_CODES = setOf(
    "ERROR_CODE_PARSING_CONTAINER_MALFORMED",
    "ERROR_CODE_IO_BAD_HTTP_STATUS",
    "ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE",
)

/**
 * Ошибка чтения/разбора, случившаяся на самом конце потока: серию уже досмотрели, ретраи и экран
 * ошибки здесь только мешают. Именно так выглядели отчёты Kodik MALFORMED: позиция ошибки равнялась
 * длительности манифеста.
 */
fun isEndOfStreamError(errorCode: String?, positionMs: Long, durationMs: Long): Boolean =
    errorCode in END_OF_STREAM_ERROR_CODES &&
        durationMs > 0L &&
        durationMs - positionMs <= PLAYER_END_ERROR_TOLERANCE_MS
