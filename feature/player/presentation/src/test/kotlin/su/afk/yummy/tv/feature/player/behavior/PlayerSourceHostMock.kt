package su.afk.yummy.tv.feature.player.behavior

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.host.PlayerChangePlayerHint
import su.afk.yummy.tv.feature.player.host.PlayerSourceHost
import su.afk.yummy.tv.feature.player.host.PlayerStreamLoadRequest

/**
 * mockk-хост для поведений источника: состояние ведёт как настоящий хост (через reducer),
 * параметры [PlayerSourceHost.loadStream] копит в [loadRequests], остальные вызовы проверяются `verify`.
 */
internal class PlayerSourceHostMock(
    scope: CoroutineScope,
    initial: PlayerState.State,
) {
    var state: PlayerState.State = initial
        private set

    val loadRequests = mutableListOf<PlayerStreamLoadRequest>()

    val host: PlayerSourceHost = mockk(relaxed = true) {
        every { this@mockk.scope } returns scope
        every { this@mockk.state } answers { this@PlayerSourceHostMock.state }
        every { update(any()) } answers { this@PlayerSourceHostMock.update(firstArg()) }
        every { changePlayerHint } returns PlayerChangePlayerHint(scope, this@PlayerSourceHostMock::update)
        every { loadStream(capture(loadRequests)) } just Runs
        every { streamErrorMessage() } returns STREAM_ERROR
    }

    fun update(reducer: PlayerState.State.() -> PlayerState.State) {
        state = state.reducer()
    }

    companion object {
        const val STREAM_ERROR = "stream error"
    }
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
