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
 * См. `docs/player-buffering.md`.
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
    SMALL(25_000, 45_000, 2_500, 5_000, 32 * 1024 * 1024),
    MEDIUM(30_000, 90_000, 2_500, 5_000, 80 * 1024 * 1024),
    LARGE(45_000, 180_000, 2_500, 5_000, 128 * 1024 * 1024),
}
