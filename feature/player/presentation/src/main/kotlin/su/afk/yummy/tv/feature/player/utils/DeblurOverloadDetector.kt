package su.afk.yummy.tv.feature.player.utils

/**
 * Определяет, что устройство не вывозит видеоэффект Deblur: декодер массово пропускает кадры,
 * то есть картинка отстаёт от звука. Сигналом служат отчёты о пропущенных кадрах за скользящее окно.
 *
 * Сразу после [reset] (включение Deblur, перемотка, новый поток) кадры игнорируются: перезапуск
 * конвейера даёт пропуски и на здоровом устройстве. Сработать может один раз за жизнь детектора,
 * чтобы не спамить пользователя тостом.
 */
class DeblurOverloadDetector {
    private val dropped = ArrayDeque<Pair<Long, Int>>()
    private var warmupUntilMs = 0L
    private var notified = false

    /** Начинает прогрев: накопленные пропуски сбрасываются, ближайшие [WARMUP_MS] не учитываются. */
    fun reset(nowMs: Long) {
        dropped.clear()
        warmupUntilMs = nowMs + WARMUP_MS
    }

    /** Отчёт декодера: [count] кадров пропущено. Возвращает `true` один раз, когда пора показать тост. */
    fun onDroppedFrames(count: Int, nowMs: Long): Boolean {
        if (notified || count <= 0 || nowMs < warmupUntilMs) return false
        dropped.addLast(nowMs to count)
        while (dropped.isNotEmpty() && nowMs - dropped.first().first > WINDOW_MS) dropped.removeFirst()
        if (dropped.sumOf { it.second } < DROPPED_FRAMES_LIMIT) return false
        notified = true
        return true
    }

    companion object {
        const val WINDOW_MS = 10_000L
        const val WARMUP_MS = 5_000L
        const val DROPPED_FRAMES_LIMIT = 60
    }
}
