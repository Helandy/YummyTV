package su.afk.yummy.tv.feature.player.handler

import javax.inject.Inject
import kotlin.math.abs

/** Owns mutable state for the current Alloha fresh-session recovery cycle. */
internal class PlayerAllohaRecoveryHandler @Inject constructor() {
    var isRecovering: Boolean = false
        private set
    var retryCount: Int = 0
        private set
    var selectedQuality: String? = null
    var positionMs: Long = 0L

    /**
     * Last failure seen in this cycle (playback error or failed resolve), for the "recovery exhausted"
     * report: without it the report says only that the attempts ran out, not why playback kept failing.
     */
    var lastFailure: String? = null

    /** Time source; replaced in tests. */
    internal var clock: () -> Long = System::currentTimeMillis

    private var lastRecovery: FinishedRecovery? = null

    /**
     * Starts a recovery cycle. A new error right after a recovery that "succeeded" at the same place
     * means the fresh link does not play either, so the new cycle continues the attempt count of the
     * previous one instead of starting from zero: otherwise the budget never runs out and every cycle
     * opens one more session (observed: ~900 `bnsi` calls in 24 minutes against a blocked session).
     */
    fun start(positionMs: Long, selectedQuality: String?) {
        val position = positionMs.coerceAtLeast(0L)
        val previous = lastRecovery
        val continuesFailure = previous != null &&
            clock() - previous.finishedAtMs <= STREAK_WINDOW_MS &&
            abs(position - previous.positionMs) <= STREAK_POSITION_MS
        isRecovering = true
        retryCount = if (continuesFailure) previous.attempts else 0
        this.selectedQuality = selectedQuality
        this.positionMs = position
    }

    fun nextAttempt(): Int = ++retryCount

    /**
     * Recovery used to retry without any limit, so a source that cannot come back left the "cannot
     * resume playback" overlay up forever with no way out. Bounded now, so the UI can fall through to
     * a real error with the change-player / change-dubbing actions.
     */
    fun canRetry(): Boolean = retryCount < MAX_ATTEMPTS

    /** The cycle ended with a resolved stream; returns how many attempts it took. */
    fun complete(): Int {
        val attempts = retryCount
        val position = positionMs
        val wasRecovering = isRecovering
        reset()
        if (wasRecovering) lastRecovery = FinishedRecovery(attempts, clock(), position)
        return attempts
    }

    /** Forgets the cycle and its streak: the source or episode changed, or the attempts ran out. */
    fun reset() {
        isRecovering = false
        retryCount = 0
        selectedQuality = null
        positionMs = 0L
        lastFailure = null
        lastRecovery = null
    }

    private data class FinishedRecovery(val attempts: Int, val finishedAtMs: Long, val positionMs: Long)

    companion object {
        const val MAX_ATTEMPTS = 4

        // A new error this soon after a recovery, at about the same position, is the same failure.
        private const val STREAK_WINDOW_MS = 60_000L
        private const val STREAK_POSITION_MS = 15_000L
    }
}
