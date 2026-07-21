package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.session.AllohaPlaybackSessionManager
import javax.inject.Inject

/** Activates, refreshes and releases the currently resolved Alloha playback session. */
internal class PlayerAllohaSessionHandler @Inject constructor(
    private val sessionManager: AllohaPlaybackSessionManager,
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
                }
            }
        }
    }

    fun selectQuality(quality: String) {
        activeSession?.selectQuality(quality)
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
    }
}
