package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/**
 * Deblur без GL-конвейера: при выключенной настройке эффекты плееру не передаются вовсе, включение на лету
 * перезапускает поток, выключение после включения пересобирает плеер.
 */
class PlayerVideoDeblurTest : BaseUnitTest() {

    private val player: ExoPlayer = mockk(relaxed = true)
    private val effects: List<Effect> = listOf(mockk())
    private val enabled = MutableStateFlow(false)
    private var rebuildRequests = 0
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    @Before
    fun setUp() {
        stubPlaying(player)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `disabled deblur never touches video effects of a plain player`() {
        createDeblur(pipelineReady = false)

        verify(exactly = 0) { player.setVideoEffects(any()) }
        assertEquals(0, rebuildRequests)
    }

    @Test
    fun `player built with the pipeline gets effects without restarting the stream`() {
        enabled.value = true

        createDeblur(pipelineReady = true)

        verify { player.setVideoEffects(effects) }
        verify(exactly = 0) { player.stop() }
        assertEquals(0, rebuildRequests)
    }

    @Test
    fun `enabling on the fly sets effects and restarts the stream at the same position`() {
        createDeblur(pipelineReady = false)

        enabled.value = true

        verifyOrder {
            player.setVideoEffects(effects)
            player.stop()
            player.seekTo(POSITION_MS)
            player.prepare()
            player.playWhenReady = true
        }
        assertEquals(0, rebuildRequests)
    }

    @Test
    fun `enabling on an idle player sets effects without restarting`() {
        every { player.playbackState } returns Player.STATE_IDLE
        createDeblur(pipelineReady = false)

        enabled.value = true

        verify { player.setVideoEffects(effects) }
        verify(exactly = 0) { player.stop() }
        verify(exactly = 0) { player.prepare() }
    }

    @Test
    fun `disabling after enabling asks for a rebuild instead of clearing effects`() {
        createDeblur(pipelineReady = false)
        enabled.value = true

        enabled.value = false

        assertEquals(1, rebuildRequests)
        verify(exactly = 0) { player.setVideoEffects(emptyList()) }
    }

    @Test
    fun `disabling a player built with the pipeline asks for a rebuild`() {
        enabled.value = true
        createDeblur(pipelineReady = true)

        enabled.value = false

        assertEquals(1, rebuildRequests)
    }

    @Test
    fun `after a rebuild deblur works with the new player`() {
        val rebuilt: ExoPlayer = mockk(relaxed = true)
        stubPlaying(rebuilt)
        val deblur = createDeblur(pipelineReady = false)
        enabled.value = true
        enabled.value = false
        deblur.attach(rebuilt, pipelineReady = false)

        enabled.value = true

        verify { rebuilt.setVideoEffects(effects) }
        verify { rebuilt.stop() }
        verify(exactly = 1) { player.setVideoEffects(any()) }
        assertEquals(1, rebuildRequests)
    }

    private fun createDeblur(pipelineReady: Boolean) = PlayerVideoDeblur(
        player = player,
        pipelineReady = pipelineReady,
        effects = effects,
        rebuildPlayer = { rebuildRequests++ },
    ).also { it.start(scope, enabled) }

    private fun stubPlaying(target: ExoPlayer) {
        every { target.playbackState } returns Player.STATE_READY
        every { target.currentMediaItem } returns mockk<MediaItem>()
        every { target.currentPosition } returns POSITION_MS
        every { target.playWhenReady } returns true
    }

    private companion object {
        const val POSITION_MS = 49_331L
    }
}
