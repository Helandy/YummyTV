package su.afk.yummy.tv.feature.player.behavior

import kotlinx.coroutines.CoroutineScope
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.host.PlayerChangePlayerHint
import su.afk.yummy.tv.feature.player.host.PlayerSourceHost
import su.afk.yummy.tv.feature.player.host.PlayerStreamLoadRequest

internal class FakePlayerSourceHost(
    override val scope: CoroutineScope,
    initial: PlayerState.State,
) : PlayerSourceHost {
    override var state: PlayerState.State = initial
        private set
    override val changePlayerHint = PlayerChangePlayerHint(scope, ::update)

    val loadRequests = mutableListOf<PlayerStreamLoadRequest>()
    var cancelledLoads = 0
        private set
    var closedSessions = 0
        private set

    override fun update(reducer: PlayerState.State.() -> PlayerState.State) {
        state = state.reducer()
    }

    override fun loadStream(request: PlayerStreamLoadRequest) {
        loadRequests += request
    }

    override fun cancelStreamLoad() {
        cancelledLoads++
    }

    override fun closeSourceSessions() {
        closedSessions++
    }

    override fun streamErrorMessage(): String = STREAM_ERROR

    companion object {
        const val STREAM_ERROR = "stream error"
    }
}

internal object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(eventName: String, params: Map<String, String>) = Unit
    override fun reportError(message: String, throwable: Throwable, groupIdentifier: String?) = Unit
    override fun log(tag: String, throwable: Throwable?, message: () -> String) = Unit
}

/** Онлайн-источник с одной серией у балансера [balancer]. */
internal fun onlineState(
    balancer: String,
    iframeUrl: String = "https://$balancer.example/episode-1",
) = PlayerState.State(
    animeId = 42,
    streamUrl = "https://cdn.example/master.m3u8",
    sourceGraph = PlayerSourceGraph(
        balancers = listOf(
            PlayerSourceBalancer(
                name = balancer,
                dubbings = listOf(
                    PlayerSourceDubbing(
                        name = "Dream Cast",
                        episodes = listOf(PlayerSourceEpisode(id = 1, number = "1", iframeUrl = iframeUrl)),
                    ),
                ),
            ),
        ),
    ),
)

internal fun playbackError(positionMs: Long = 0L) =
    PlayerState.Event.PlaybackError(message = "Source error", positionMs = positionMs)
