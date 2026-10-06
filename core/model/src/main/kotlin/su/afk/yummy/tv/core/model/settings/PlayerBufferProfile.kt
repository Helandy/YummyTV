package su.afk.yummy.tv.core.model.settings

/**
 * Пресет оперативного буфера ExoPlayer: сколько секунд видео плеер держит впереди позиции
 * воспроизведения и сколько памяти под это отводит. Это не дисковый кэш и не офлайн-загрузка.
 *
 * Инварианты DefaultLoadControl: bufferForPlayback(AfterRebuffer)Ms <= minBufferMs <= maxBufferMs.
 *
 * Память ограничена [targetBufferBytes] и [maxBufferMs] (размер главнее времени), а [minBufferMs] -
 * это порог, ниже которого загрузчик снова начинает подкачку, то есть гарантированный минимум запаса
 * после первого заполнения. Поэтому повышение минимума потолок ОЗУ не двигает.
 *
 * У Alloha прокси держит запрос сегмента до 14 с, пока обновляется сессия
 * (`AllohaStreamProxy.SEGMENT_HOLD_FOR_REFRESH_MS`), и плеер всё это время доигрывает буфер: минимум
 * должен быть заметно больше 14 с. [SMALL] и [MEDIUM] это покрывают. [MINIMAL] (максимум 15 с) -
 * принципиально нет: у него удержание способно опустошить буфер. См. `docs/player-buffering.md`.
 */
enum class PlayerBufferProfile(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
    val targetBufferBytes: Int,
) {
    MINIMAL(5_000, 15_000, 1_500, 3_000, 16 * 1024 * 1024),

    /** Профиль по умолчанию. */
    SMALL(18_000, 30_000, 2_500, 5_000, 24 * 1024 * 1024),
    MEDIUM(25_000, 60_000, 2_500, 5_000, 64 * 1024 * 1024),
    LARGE(30_000, 120_000, 2_500, 5_000, 96 * 1024 * 1024),
}
