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
import su.afk.yummy.tv.core.model.settings.VideoEnhancementMode
import su.afk.yummy.tv.core.testing.BaseUnitTest

/**
 * Режимы улучшения видео без лишнего GL-конвейера: выключенный режим эффекты плееру не передаёт вовсе, включение
 * на лету перезапускает поток, смена режима при готовом конвейере только подменяет эффекты, выключение после
 * включения пересобирает плеер.
 */
class PlayerVideoEnhancementTest : BaseUnitTest() {

    private val player: ExoPlayer = mockk(relaxed = true)
    private val sharpenEffects: List<Effect> = listOf(mockk())
    private val qualityEffects: List<Effect> = listOf(mockk())
    private val mode = MutableStateFlow(VideoEnhancementMode.OFF)
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
    fun `off mode never touches video effects of a plain player`() {
        createEnhancement(pipelineReady = false)

        verify(exactly = 0) { player.setVideoEffects(any()) }
        assertEquals(0, rebuildRequests)
    }

    @Test
    fun `player built with the pipeline gets effects without restarting the stream`() {
        mode.value = VideoEnhancementMode.QUALITY

        createEnhancement(pipelineReady = true)

        verify { player.setVideoEffects(qualityEffects) }
        verify(exactly = 0) { player.stop() }
        assertEquals(0, rebuildRequests)
    }

    @Test
    fun `enabling on the fly sets effects and restarts the stream at the same position`() {
        createEnhancement(pipelineReady = false)

        changeMode(VideoEnhancementMode.SHARPEN)

        verifyOrder {
            player.setVideoEffects(sharpenEffects)
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
        createEnhancement(pipelineReady = false)

        changeMode(VideoEnhancementMode.QUALITY)

        verify { player.setVideoEffects(qualityEffects) }
        verify(exactly = 0) { player.stop() }
        verify(exactly = 0) { player.prepare() }
    }

    @Test
    fun `switching between modes only replaces the effects`() {
        createEnhancement(pipelineReady = false)
        changeMode(VideoEnhancementMode.SHARPEN)

        changeMode(VideoEnhancementMode.QUALITY)

        verify { player.setVideoEffects(qualityEffects) }
        verify(exactly = 1) { player.stop() }
        assertEquals(0, rebuildRequests)
    }

    @Test
    fun `turning off after enabling asks for a rebuild instead of clearing effects`() {
        createEnhancement(pipelineReady = false)
        changeMode(VideoEnhancementMode.SHARPEN)

        changeMode(VideoEnhancementMode.OFF)

        assertEquals(1, rebuildRequests)
        verify(exactly = 0) { player.setVideoEffects(emptyList()) }
    }

    @Test
    fun `turning off a player built with the pipeline asks for a rebuild`() {
        mode.value = VideoEnhancementMode.QUALITY
        createEnhancement(pipelineReady = true)

        changeMode(VideoEnhancementMode.OFF)

        assertEquals(1, rebuildRequests)
    }

    @Test
    fun `current mode follows the setting`() {
        val enhancement = createEnhancement(pipelineReady = false)

        changeMode(VideoEnhancementMode.QUALITY)

        assertEquals(VideoEnhancementMode.QUALITY, enhancement.mode)
    }

    @Test
    fun `after a rebuild the new player gets the next mode`() {
        val rebuilt: ExoPlayer = mockk(relaxed = true)
        stubPlaying(rebuilt)
        val enhancement = createEnhancement(pipelineReady = false)
        changeMode(VideoEnhancementMode.SHARPEN)
        changeMode(VideoEnhancementMode.OFF)
        enhancement.attach(rebuilt, pipelineReady = false)

        changeMode(VideoEnhancementMode.QUALITY)

        verify { rebuilt.setVideoEffects(qualityEffects) }
        verify { rebuilt.stop() }
        verify(exactly = 1) { player.setVideoEffects(any()) }
        assertEquals(1, rebuildRequests)
    }

    private fun createEnhancement(pipelineReady: Boolean) = PlayerVideoEnhancement(
        player = player,
        pipelineReady = pipelineReady,
        effectsFor = { mode ->
            when (mode) {
                VideoEnhancementMode.OFF -> emptyList()
                VideoEnhancementMode.SHARPEN -> sharpenEffects
                VideoEnhancementMode.QUALITY -> qualityEffects
            }
        },
        rebuildPlayer = { rebuildRequests++ },
    ).also { it.start(scope, mode) }

    /** Меняет режим и проматывает паузу перед применением (debounce в [PlayerVideoEnhancement]). */
    private fun changeMode(value: VideoEnhancementMode) {
        mode.value = value
        testScheduler.advanceTimeBy(APPLY_DEBOUNCE_MS + 1)
        testScheduler.runCurrent()
    }

    private fun stubPlaying(target: ExoPlayer) {
        every { target.playbackState } returns Player.STATE_READY
        every { target.currentMediaItem } returns mockk<MediaItem>()
        every { target.currentPosition } returns POSITION_MS
        every { target.playWhenReady } returns true
    }

    private companion object {
        const val POSITION_MS = 49_331L
        const val APPLY_DEBOUNCE_MS = 1_500L
    }
}
