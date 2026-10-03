package su.afk.yummy.tv.data.player.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.repository.AllohaPlaybackSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultAllohaPlaybackSessionManager @Inject constructor(
    @IoApplicationScope private val scope: CoroutineScope,
) : AllohaPlaybackSessionRepository {
    private val lock = Any()
    private var activeSession: AllohaStreamSession? = null
    private var pendingRelease: Job? = null

    override fun find(sourceKey: String): AllohaStreamSession? = synchronized(lock) {
        activeSession?.takeIf { it.sourceKey == sourceKey }?.also {
            pendingRelease?.cancel()
            pendingRelease = null
        }
    }

    override fun activate(session: AllohaStreamSession): AllohaStreamSession = synchronized(lock) {
        pendingRelease?.cancel()
        pendingRelease = null
        if (activeSession !== session) activeSession?.close()
        activeSession = session
        session
    }

    override fun release(session: AllohaStreamSession, immediately: Boolean) {
        synchronized(lock) {
            if (activeSession !== session) {
                session.close()
                return
            }
            pendingRelease?.cancel()
            if (immediately) {
                activeSession = null
                pendingRelease = null
                session.close()
            } else {
                pendingRelease = scope.launch {
                    delay(CONFIGURATION_CHANGE_GRACE_MS)
                    synchronized(lock) {
                        if (activeSession === session) {
                            activeSession = null
                            pendingRelease = null
                            session.close()
                        }
                    }
                }
            }
        }
    }

    override fun closeActive() {
        synchronized(lock) {
            pendingRelease?.cancel()
            pendingRelease = null
            activeSession?.close()
            activeSession = null
        }
    }

    private companion object {
        const val CONFIGURATION_CHANGE_GRACE_MS = 10_000L
    }
}
