package su.afk.yummy.tv.feature.player.handler

import javax.inject.Inject

/** Owns mutable state for the current Alloha fresh-session recovery cycle. */
internal class PlayerAllohaRecoveryHandler @Inject constructor() {
    var isRecovering: Boolean = false
        private set
    var retryCount: Int = 0
        private set
    var selectedQuality: String? = null
    var positionMs: Long = 0L

    fun start(positionMs: Long, selectedQuality: String?) {
        isRecovering = true
        retryCount = 0
        this.selectedQuality = selectedQuality
        this.positionMs = positionMs.coerceAtLeast(0L)
    }

    fun nextAttempt(): Int = ++retryCount

    fun complete(): Int = retryCount.also { reset() }

    fun reset() {
        isRecovering = false
        retryCount = 0
        selectedQuality = null
        positionMs = 0L
    }
}
