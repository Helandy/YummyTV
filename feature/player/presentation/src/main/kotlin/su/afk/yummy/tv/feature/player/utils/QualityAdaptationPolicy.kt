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
    /**
     * Играет синтетический master: вариант по сети выбирает ABR плеера, а политика держит только потолок
     * по возможностям устройства. Остановки и оценка полосы потолок не трогают (иначе он держал бы качество
     * ниже, чем уже готов играть ABR), а после истечения лимита устройства потолок возвращается сразу.
     */
    private val deviceOnly: Boolean = false,
) {
    private val stallTimesMs = ArrayDeque<Long>()
    private val droppedFrames = ArrayDeque<Pair<Long, Int>>()

    /** Высота → до какого момента повышение до неё запрещено (после падения по сети). */
    private val networkBanUntilMs = mutableMapOf<Int, Long>()

    /** Высота → сколько раз подряд с неё падали по сети: бан растёт, чтобы не качаться вверх-вниз. */
    private val networkStrikes = mutableMapOf<Int, Int>()

    /** Устройство не тянет эту высоту и выше: к ним не поднимаемся, пока не истёк [deviceLimitUntilMs]. */
    private var deviceLimitHeight: Int? = null
    private var deviceLimitUntilMs = 0L

    /** Счётчик оценок полосы и тик, на котором приходила последняя: по ним видно, что оценка протухла. */
    private var bandwidthSamples = 0
    private var seenBandwidthSamples = 0
    private var lastFreshBandwidthAtMs = Long.MIN_VALUE / 2

    private var lastSwitchAtMs = Long.MIN_VALUE / 2
    private var lastBandwidthBps = 0L
    private var goodBandwidthSinceMs: Long? = null

    /** Начало, пока не закончившейся, остановки буфера; null — сейчас играем. */
    private var stallStartedAtMs: Long? = null
    private var stallLimitMs = LONG_STALL_MS

    /** Текущая остановка — буферизация после перемотки: её срок зависит от оценки полосы, см. [evaluate]. */
    private var stallAfterSeek = false

    /**
     * Начало остановки буфера (не перемотка). [startup] — серия ещё ни разу не начинала играть:
     * такая остановка в счётчик повторных не идёт, но затянувшийся старт понижает качество
     * (на слабой сети лучшее качество может не стартовать вовсе). [afterSeek] — буферизация после
     * перемотки или замены item: короткая нормальна и в счётчик повторных не идёт, но затянувшаяся
     * (сервер не отдаёт нужный кусок) понижает качество так же, как обычная долгая остановка.
     */
    fun onStallStarted(nowMs: Long, startup: Boolean = false, afterSeek: Boolean = false) {
        if (!startup && !afterSeek) stallTimesMs.addLast(nowMs)
        stallStartedAtMs = nowMs
        stallAfterSeek = afterSeek && !startup
        stallLimitMs = when {
            startup -> LONG_STARTUP_MS
            // Замена item при смене качества тоже даёт «перемотку»: новому потоку остаётся его
            // пауза на загрузку, а не короткий порог.
            afterSeek && nowMs - lastSwitchAtMs < LONG_AFTER_SWITCH_MS -> LONG_AFTER_SWITCH_MS
            afterSeek -> LONG_SEEK_STALL_MS
            else -> LONG_STALL_MS
        }
        trim(nowMs)
    }

    /** Остановка закончилась: воспроизведение возобновилось или остановлено. */
    fun onStallEnded() {
        stallStartedAtMs = null
        stallAfterSeek = false
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
        bandwidthSamples++
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
            stallAfterSeek = false
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
        if (bandwidthSamples != seenBandwidthSamples) {
            seenBandwidthSamples = bandwidthSamples
            lastFreshBandwidthAtMs = nowMs
        }
        if (nowMs - lastSwitchAtMs >= STRIKES_RESET_MS && stallTimesMs.isEmpty()) {
            networkStrikes.remove(currentHeight)
        }

        if (deviceOnly) return evaluateDeviceOnly(currentHeight, levels, nowMs)

        // С какого тика последняя оценка полосы покрывает с запасом хотя бы одну ступень выше.
        val upTarget = upgradeTarget(currentHeight, available, nowMs)
        goodBandwidthSinceMs = if (upTarget != null) goodBandwidthSinceMs ?: nowMs else null

        val deviceStruggles = droppedFrames.sumOf { it.second } >= DEVICE_DROPPED_FRAMES_LIMIT
        // Одна затянувшаяся остановка — тот же сигнал, что и две короткие: второго захода в
        // буферизацию на очень медленной сети можно не дождаться.
        val longStall = stallStartedAtMs?.let { nowMs - it >= effectiveStallLimitMs(currentHeight) } == true
        val networkStruggles = stallTimesMs.size >= NETWORK_STALLS_LIMIT || longStall
        if (deviceStruggles || networkStruggles) {
            val lower = levels.filter { it.second < currentHeight }.maxByOrNull { it.second }
                ?: return QualityAdaptationDecision.Keep
            if (deviceStruggles) {
                deviceLimitHeight = minOf(activeDeviceLimit(nowMs) ?: currentHeight, currentHeight)
                deviceLimitUntilMs = nowMs + DEVICE_LIMIT_MS
            } else {
                val strikes = networkStrikes[currentHeight] ?: 0
                networkStrikes[currentHeight] = strikes + 1
                networkBanUntilMs[currentHeight] = nowMs + networkBanMs(strikes)
            }
            return QualityAdaptationDecision.Switch(lower.first, isDowngrade = true)
        }

        if (nowMs - lastSwitchAtMs < MIN_SWITCH_INTERVAL_MS) return QualityAdaptationDecision.Keep
        if (stallTimesMs.isNotEmpty() || droppedFrames.isNotEmpty()) return QualityAdaptationDecision.Keep
        if (bufferedAheadMs < UP_MIN_BUFFER_MS) return QualityAdaptationDecision.Keep
        val goodSince = goodBandwidthSinceMs
        val estimateCovers = goodSince != null && nowMs - goodSince >= UP_STABLE_MS
        val higher = (if (estimateCovers) upTarget else probeTarget(currentHeight, available, bufferedAheadMs, nowMs))
            ?: return QualityAdaptationDecision.Keep
        val label = levels.first { it.second == higher }.first
        return QualityAdaptationDecision.Switch(label, isDowngrade = false)
    }

    /** Режим [deviceOnly]: понижаем только по дропу кадров, возвращаем потолок после истечения лимита устройства. */
    private fun evaluateDeviceOnly(
        currentHeight: Int,
        levels: List<Pair<String, Int>>,
        nowMs: Long,
    ): QualityAdaptationDecision {
        if (droppedFrames.sumOf { it.second } >= DEVICE_DROPPED_FRAMES_LIMIT) {
            val lower = levels.filter { it.second < currentHeight }.maxByOrNull { it.second }
                ?: return QualityAdaptationDecision.Keep
            deviceLimitHeight = minOf(activeDeviceLimit(nowMs) ?: currentHeight, currentHeight)
            deviceLimitUntilMs = nowMs + DEVICE_LIMIT_MS
            return QualityAdaptationDecision.Switch(lower.first, isDowngrade = true)
        }
        if (deviceLimitHeight == null || activeDeviceLimit(nowMs) != null) return QualityAdaptationDecision.Keep
        if (nowMs - lastSwitchAtMs < MIN_SWITCH_INTERVAL_MS || droppedFrames.isNotEmpty()) {
            return QualityAdaptationDecision.Keep
        }
        val higher = levels.filter { it.second in (currentHeight + 1)..ceilingHeight }.maxByOrNull { it.second }
            ?: return QualityAdaptationDecision.Keep
        return QualityAdaptationDecision.Switch(higher.first, isDowngrade = false)
    }

    /**
     * Проба на одну ступень выше без свежей оценки полосы. При полном буфере ExoPlayer почти не
     * качает, и оценка остаётся заниженной с момента проблем, хотя сеть давно восстановилась.
     * Если долго играем без остановок и буфер большой — пробуем ступень выше; неудача даст
     * понижение и растущий бан этой высоты.
     */
    private fun probeTarget(height: Int, available: List<String>, bufferedAheadMs: Long, nowMs: Long): Int? {
        if (stallStartedAtMs != null) return null
        if (bufferedAheadMs < UP_PROBE_BUFFER_MS) return null
        if (nowMs - lastSwitchAtMs < UP_PROBE_STABLE_MS) return null
        if (nowMs - lastFreshBandwidthAtMs < STALE_BANDWIDTH_MS) return null
        return eligibleHeights(height, available, nowMs).minOrNull()
    }

    private fun eligibleHeights(height: Int, available: List<String>, nowMs: Long): List<Int> {
        val deviceLimit = activeDeviceLimit(nowMs)
        return available
            .mapNotNull { it.qualityHeight() }
            .filter { it > height && it <= ceilingHeight }
            .filter { candidate -> deviceLimit?.let { candidate < it } ?: true }
            .filter { candidate -> (networkBanUntilMs[candidate] ?: 0L) <= nowMs }
    }

    private fun activeDeviceLimit(nowMs: Long): Int? = deviceLimitHeight?.takeIf { nowMs < deviceLimitUntilMs }

    private fun networkBanMs(strikes: Int): Long =
        (NETWORK_BAN_MS shl strikes.coerceAtMost(MAX_BAN_DOUBLINGS)).coerceAtMost(MAX_NETWORK_BAN_MS)

    /**
     * Срок затянувшейся остановки. После перемотки (в т.ч. пропуска опенинга) буфер пуст и часто
     * открывается новое соединение: задержка там не про полосу. Если оценка полосы тянет текущее
     * качество, ждём дольше; плохая оценка — прежний короткий срок; оценки нет — как обычная остановка.
     */
    private fun effectiveStallLimitMs(currentHeight: Int): Long {
        if (!stallAfterSeek || stallLimitMs != LONG_SEEK_STALL_MS) return stallLimitMs
        return when {
            lastBandwidthBps <= 0L -> LONG_STALL_MS
            lastBandwidthBps >= nominalBitrateBps(currentHeight) * UP_BANDWIDTH_FACTOR -> LONG_SEEK_STALL_HEALTHY_MS
            else -> LONG_SEEK_STALL_MS
        }
    }

    /**
     * Наивысшая высота выше [height], на которую хватает полосы с запасом: в пределах потолка, без
     * банов. Поднимаемся сразу на неё, а не по ступеньке, иначе возврат после падения затягивается.
     */
    private fun upgradeTarget(height: Int, available: List<String>, nowMs: Long): Int? =
        eligibleHeights(height, available, nowMs)
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

        /** Буферизация после перемотки дольше этого срока: нужный кусок сеть не тянет. */
        const val LONG_SEEK_STALL_MS = 5_000L

        /** То же, но оценка полосы тянет текущее качество: задержка — это открытие соединения, а не сеть. */
        const val LONG_SEEK_STALL_HEALTHY_MS = 10_000L

        /** Старт воспроизведения дольше этого срока — лучшее качество сети не по силам. */
        const val LONG_STARTUP_MS = 15_000L

        /** Сколько ждать новую загрузку после смены качества, прежде чем снова понижать. */
        const val LONG_AFTER_SWITCH_MS = 25_000L

        /** 30 пропущенных кадров за 10 с (~3 к/с) — декодер или GPU не успевают. */
        const val DEVICE_DROPPED_FRAMES_LIMIT = 30
        const val DROPPED_WINDOW_MS = 10_000L

        /** Первый бан высоты после падения по сети; дальше растёт вдвое до [MAX_NETWORK_BAN_MS]. */
        const val NETWORK_BAN_MS = 30_000L
        const val MAX_NETWORK_BAN_MS = 300_000L
        private const val MAX_BAN_DOUBLINGS = 4

        /** Сколько играть на высоте без остановок, чтобы забыть её прошлые падения. */
        const val STRIKES_RESET_MS = 120_000L

        /** Лимит устройства по дропу кадров истекает: нагрузка и нагрев меняются. */
        const val DEVICE_LIMIT_MS = 180_000L

        /** Оценка полосы старше этого срока считается протухшей. */
        const val STALE_BANDWIDTH_MS = 15_000L

        /** Условия пробы ступени выше при протухшей оценке. */
        const val UP_PROBE_BUFFER_MS = 20_000L
        const val UP_PROBE_STABLE_MS = 20_000L
        const val MIN_SWITCH_INTERVAL_MS = 15_000L
        const val UP_STABLE_MS = 8_000L
        const val UP_MIN_BUFFER_MS = 10_000L
        const val UP_BANDWIDTH_FACTOR = 1.4

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
