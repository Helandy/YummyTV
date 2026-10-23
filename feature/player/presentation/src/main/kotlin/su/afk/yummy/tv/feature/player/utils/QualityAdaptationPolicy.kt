package su.afk.yummy.tv.feature.player.utils

/** Решение адаптивного качества: остаться или перейти на [Switch.label]. */
sealed interface QualityAdaptationDecision {
    data object Keep : QualityAdaptationDecision

    data class Switch(val label: String, val isDowngrade: Boolean) : QualityAdaptationDecision
}

/**
 * Решает, когда в режиме «Адаптивное» понижать качество потока и когда возвращать обратно.
 *
 * У большинства источников каждое качество — отдельная ссылка без ABR, поэтому смена качества
 * перезапускает media item. Отсюда гистерезис: понижаем быстро по фактам (повторные остановки
 * буфера или дроп кадров), повышаем медленно и только при устойчивом запасе пропускной способности.
 *
 * Состояние чисто временное, часы приходят параметром [nowMs] — класс не зависит от Android.
 * Одна политика живёт на одну серию: при смене потока создаётся новая.
 */
class QualityAdaptationPolicy(
    /** Верхняя граница: выше этой высоты не поднимаемся (по умолчанию — до 4K, если сеть и декодер тянут). */
    private val ceilingHeight: Int = DEFAULT_CEILING_HEIGHT,
) {
    private val stallTimesMs = ArrayDeque<Long>()
    private val droppedFrames = ArrayDeque<Pair<Long, Int>>()

    /** Высота → до какого момента повышение до неё запрещено (после падения по сети). */
    private val networkBanUntilMs = mutableMapOf<Int, Long>()

    /** Устройство не тянет эту высоту и выше: до конца серии не поднимаемся к ним. */
    private var deviceLimitHeight: Int? = null

    private var lastSwitchAtMs = Long.MIN_VALUE / 2
    private var lastBandwidthBps = 0L
    private var goodBandwidthSinceMs: Long? = null

    /** Начало, пока не закончившейся, остановки буфера; null — сейчас играем. */
    private var stallStartedAtMs: Long? = null
    private var stallLimitMs = LONG_STALL_MS

    /**
     * Начало остановки буфера (не перемотка). [startup] — серия ещё ни разу не начинала играть:
     * такая остановка в счётчик повторных не идёт, но затянувшийся старт понижает качество
     * (на слабой сети лучшее качество может не стартовать вовсе).
     */
    fun onStallStarted(nowMs: Long, startup: Boolean = false) {
        if (!startup) stallTimesMs.addLast(nowMs)
        stallStartedAtMs = nowMs
        stallLimitMs = if (startup) LONG_STARTUP_MS else LONG_STALL_MS
        trim(nowMs)
    }

    /** Остановка закончилась: воспроизведение возобновилось или остановлено. */
    fun onStallEnded() {
        stallStartedAtMs = null
    }

    /** Отчёт декодера: [count] кадров пропущено с прошлого отчёта. */
    fun onDroppedFrames(count: Int, nowMs: Long) {
        if (count <= 0) return
        droppedFrames.addLast(nowMs to count)
        trim(nowMs)
    }

    /**
     * Оценка пропускной способности ExoPlayer, бит/с. Хранится последняя: ExoPlayer присылает её
     * редко (при полном буфере загрузок почти нет), поэтому «устойчивость» считается по тикам
     * [evaluate], а не по числу отчётов.
     */
    fun onBandwidth(bps: Long) {
        lastBandwidthBps = bps
    }

    /** Качество сменилось (нашим решением или ручным выбором): сбрасываем накопленные факты. */
    fun onQualityChanged(nowMs: Long) {
        lastSwitchAtMs = nowMs
        stallTimesMs.clear()
        droppedFrames.clear()
        goodBandwidthSinceMs = null
        // Смена качества пересоздаёт поток (у некоторых источников это секунды): новой загрузке
        // даём время, иначе одна и та же остановка каскадом столкнёт качество на несколько ступеней.
        if (stallStartedAtMs != null) {
            stallStartedAtMs = nowMs
            stallLimitMs = LONG_AFTER_SWITCH_MS
        }
    }

    /**
     * Оценка текущего состояния; вызывать периодически (раз в несколько секунд).
     *
     * @param bufferedAheadMs запас буфера впереди позиции: повышаем только при достаточном запасе,
     * иначе перезапуск потока даст заметную паузу.
     */
    fun evaluate(
        currentLabel: String?,
        available: List<String>,
        bufferedAheadMs: Long,
        nowMs: Long,
    ): QualityAdaptationDecision {
        val currentHeight = currentLabel?.qualityHeight() ?: return QualityAdaptationDecision.Keep
        val levels = available.mapNotNull { label -> label.qualityHeight()?.let { label to it } }
        if (levels.size < 2) return QualityAdaptationDecision.Keep
        trim(nowMs)

        // С какого тика последняя оценка полосы покрывает с запасом хотя бы одну ступень выше.
        val upTarget = upgradeTarget(currentHeight, available, nowMs)
        goodBandwidthSinceMs = if (upTarget != null) goodBandwidthSinceMs ?: nowMs else null

        val deviceStruggles = droppedFrames.sumOf { it.second } >= DEVICE_DROPPED_FRAMES_LIMIT
        // Одна затянувшаяся остановка — тот же сигнал, что и две короткие: второго захода в
        // буферизацию на очень медленной сети можно не дождаться.
        val longStall = stallStartedAtMs?.let { nowMs - it >= stallLimitMs } == true
        val networkStruggles = stallTimesMs.size >= NETWORK_STALLS_LIMIT || longStall
        if (deviceStruggles || networkStruggles) {
            val lower = levels.filter { it.second < currentHeight }.maxByOrNull { it.second }
                ?: return QualityAdaptationDecision.Keep
            if (deviceStruggles) {
                deviceLimitHeight = minOf(deviceLimitHeight ?: currentHeight, currentHeight)
            } else {
                networkBanUntilMs[currentHeight] = nowMs + NETWORK_BAN_MS
            }
            return QualityAdaptationDecision.Switch(lower.first, isDowngrade = true)
        }

        if (nowMs - lastSwitchAtMs < MIN_SWITCH_INTERVAL_MS) return QualityAdaptationDecision.Keep
        if (stallTimesMs.isNotEmpty() || droppedFrames.isNotEmpty()) return QualityAdaptationDecision.Keep
        if (bufferedAheadMs < UP_MIN_BUFFER_MS) return QualityAdaptationDecision.Keep
        val goodSince = goodBandwidthSinceMs ?: return QualityAdaptationDecision.Keep
        if (nowMs - goodSince < UP_STABLE_MS) return QualityAdaptationDecision.Keep

        val higher = upTarget ?: return QualityAdaptationDecision.Keep
        val label = levels.first { it.second == higher }.first
        return QualityAdaptationDecision.Switch(label, isDowngrade = false)
    }

    /**
     * Наивысшая высота выше [height], на которую хватает полосы с запасом: в пределах потолка, без
     * банов. Поднимаемся сразу на неё, а не по ступеньке, иначе возврат после падения затягивается.
     */
    private fun upgradeTarget(height: Int, available: List<String>, nowMs: Long): Int? =
        available
            .mapNotNull { it.qualityHeight() }
            .filter { it > height && it <= ceilingHeight }
            .filter { candidate -> deviceLimitHeight?.let { candidate < it } ?: true }
            .filter { candidate -> (networkBanUntilMs[candidate] ?: 0L) <= nowMs }
            .filter { candidate -> lastBandwidthBps >= nominalBitrateBps(candidate) * UP_BANDWIDTH_FACTOR }
            .maxOrNull()

    private fun trim(nowMs: Long) {
        while (stallTimesMs.isNotEmpty() && nowMs - stallTimesMs.first() > STALL_WINDOW_MS) {
            stallTimesMs.removeFirst()
        }
        while (droppedFrames.isNotEmpty() && nowMs - droppedFrames.first().first > DROPPED_WINDOW_MS) {
            droppedFrames.removeFirst()
        }
    }

    companion object {
        /**
         * Потолок адаптивного режима: 4K. С него же начинается воспроизведение (лучшее доступное
         * не выше потолка), а при затянувшемся старте, остановках или дропе кадров политика понижает.
         */
        const val DEFAULT_CEILING_HEIGHT = 2160

        /** Две остановки буфера за минуту — сеть не тянет текущее качество. */
        const val NETWORK_STALLS_LIMIT = 2
        const val STALL_WINDOW_MS = 60_000L

        /** Остановка буфера дольше этого срока сама по себе означает, что сеть не тянет. */
        const val LONG_STALL_MS = 8_000L

        /** Старт воспроизведения дольше этого срока — лучшее качество сети не по силам. */
        const val LONG_STARTUP_MS = 15_000L

        /** Сколько ждать новую загрузку после смены качества, прежде чем снова понижать. */
        const val LONG_AFTER_SWITCH_MS = 25_000L

        /** 30 пропущенных кадров за 10 с (~3 к/с) — декодер или GPU не успевают. */
        const val DEVICE_DROPPED_FRAMES_LIMIT = 30
        const val DROPPED_WINDOW_MS = 10_000L

        const val NETWORK_BAN_MS = 30_000L
        const val MIN_SWITCH_INTERVAL_MS = 15_000L
        const val UP_STABLE_MS = 8_000L
        const val UP_MIN_BUFFER_MS = 6_000L
        const val UP_BANDWIDTH_FACTOR = 1.5

        /** Ориентировочный битрейт качества, бит/с: у MP4-источников реального нет в манифесте. */
        fun nominalBitrateBps(height: Int): Long = when {
            height <= 360 -> 800_000L
            height <= 480 -> 1_200_000L
            height <= 720 -> 2_500_000L
            height <= 1080 -> 5_000_000L
            height <= 1440 -> 9_000_000L
            else -> 16_000_000L
        }
    }
}
