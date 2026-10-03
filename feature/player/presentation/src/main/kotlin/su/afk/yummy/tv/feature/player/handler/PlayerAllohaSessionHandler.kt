package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.AllohaPlaybackSessionRepository
import javax.inject.Inject

/** Activates, refreshes and releases the currently resolved Alloha playback session. */
internal class PlayerAllohaSessionHandler @Inject constructor(
    private val sessionManager: AllohaPlaybackSessionRepository,
) {
    private var activeSession: AllohaStreamSession? = null
    private var refreshJob: Job? = null

    fun activate(session: AllohaStreamSession?, scope: CoroutineScope) {
        if (activeSession === session) return
        close()
        activeSession = session?.let(sessionManager::activate) ?: return
        refreshJob = scope.launch {
            while (true) {
                val expiresAt = session.expiresAtMs()
                if (expiresAt == null) {
                    delay(SESSION_EXPIRY_POLL_MS)
                    continue
                }
                delay(
                    (expiresAt - System.currentTimeMillis() - SESSION_REFRESH_LEAD_MS)
                        .coerceAtLeast(0L)
                )
                if (activeSession === session && session.expiresAtMs() == expiresAt) {
                    session.refresh()
                    // refresh() deliberately keeps the current expiry live so the still-valid token
                    // keeps serving while the new one is staged; poll until the rotation commits a
                    // new one instead of re-entering the (now zero-length) delay above on every
                    // tick. Bounded, so a rotation that never produces a TTL falls through to
                    // another refresh attempt rather than wedging this loop.
                    val deadline = System.currentTimeMillis() + SESSION_ROTATION_WAIT_MS
                    while (
                        activeSession === session &&
                        session.expiresAtMs() == expiresAt &&
                        System.currentTimeMillis() < deadline
                    ) {
                        delay(SESSION_EXPIRY_POLL_MS)
                    }
                }
            }
        }
    }

    fun selectQuality(quality: String) {
        activeSession?.selectQuality(quality)
    }

    /**
     * Switches the Alloha dubbing inside the live session and returns the resulting stream (new URL
     * and quality ladder), so the caller can swap the MediaItem without re-extracting.
     */
    fun selectAudioTrack(audioId: String): PlayerStreamResolveResult.Stream? {
        val session = activeSession ?: return null
        session.selectAudioTrack(audioId)
        return runCatching { session.initialStream }.getOrNull()
    }

    fun close(immediately: Boolean = true) {
        refreshJob?.cancel()
        refreshJob = null
        activeSession?.let { sessionManager.release(it, immediately) }
        activeSession = null
    }

    private companion object {
        const val SESSION_REFRESH_LEAD_MS = 20_000L
        const val SESSION_EXPIRY_POLL_MS = 500L

        // Upper bound on waiting for a staged rotation to commit a new TTL. Must exceed the
        // extractor's own forced-commit timeout so the normal path always wins the race.
        const val SESSION_ROTATION_WAIT_MS = 15_000L
    }
}
