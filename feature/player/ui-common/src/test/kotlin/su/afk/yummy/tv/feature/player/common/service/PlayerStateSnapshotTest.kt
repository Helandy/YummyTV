package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/** Снимок, который переносится на пересобранный плеер при выключении Deblur на лету. */
class PlayerStateSnapshotTest : BaseUnitTest() {

    private val firstItem: MediaItem = mockk()
    private val secondItem: MediaItem = mockk()
    private val trackParameters: TrackSelectionParameters = mockk()
    private val source: Player = mockk()
    private val target: Player = mockk(relaxed = true)

    @Test
    fun `capture keeps queue position pause speed volume and track choice`() {
        stubSource(playbackState = Player.STATE_READY)

        val snapshot = PlayerStateSnapshot.capture(source)

        assertEquals(listOf(firstItem, secondItem), snapshot.mediaItems)
        assertEquals(1, snapshot.mediaItemIndex)
        assertEquals(POSITION_MS, snapshot.positionMs)
        assertFalse(snapshot.playWhenReady)
        assertTrue(snapshot.prepared)
        assertEquals(SPEED, snapshot.playbackParameters)
        assertEquals(Player.REPEAT_MODE_ONE, snapshot.repeatMode)
        assertTrue(snapshot.shuffleModeEnabled)
        assertEquals(VOLUME, snapshot.volume)
        assertEquals(trackParameters, snapshot.trackSelectionParameters)
    }

    @Test
    fun `idle player is captured as not prepared`() {
        stubSource(playbackState = Player.STATE_IDLE)

        assertFalse(PlayerStateSnapshot.capture(source).prepared)
    }

    @Test
    fun `restore applies settings then queue at the same position then prepares and resumes`() {
        stubSource(playbackState = Player.STATE_BUFFERING, playWhenReady = true)

        PlayerStateSnapshot.capture(source).restoreTo(target)

        verifyOrder {
            target.trackSelectionParameters = trackParameters
            target.playbackParameters = SPEED
            target.repeatMode = Player.REPEAT_MODE_ONE
            target.shuffleModeEnabled = true
            target.volume = VOLUME
            target.setMediaItems(listOf(firstItem, secondItem), 1, POSITION_MS)
            target.prepare()
            target.playWhenReady = true
        }
    }

    @Test
    fun `restore of an idle player does not prepare it`() {
        stubSource(playbackState = Player.STATE_IDLE)

        PlayerStateSnapshot.capture(source).restoreTo(target)

        verify { target.setMediaItems(listOf(firstItem, secondItem), 1, POSITION_MS) }
        verify(exactly = 0) { target.prepare() }
    }

    @Test
    fun `restore of an empty queue sets no items and does not prepare`() {
        stubSource(playbackState = Player.STATE_READY, items = emptyList())

        PlayerStateSnapshot.capture(source).restoreTo(target)

        verify(exactly = 0) { target.setMediaItems(any(), any(), any()) }
        verify(exactly = 0) { target.prepare() }
        verify { target.playWhenReady = false }
    }

    private fun stubSource(
        playbackState: Int,
        playWhenReady: Boolean = false,
        items: List<MediaItem> = listOf(firstItem, secondItem),
    ) {
        every { source.mediaItemCount } returns items.size
        every { source.getMediaItemAt(any()) } answers { items[firstArg()] }
        every { source.currentMediaItemIndex } returns 1
        every { source.currentPosition } returns POSITION_MS
        every { source.playWhenReady } returns playWhenReady
        every { source.playbackState } returns playbackState
        every { source.playbackParameters } returns SPEED
        every { source.repeatMode } returns Player.REPEAT_MODE_ONE
        every { source.shuffleModeEnabled } returns true
        every { source.volume } returns VOLUME
        every { source.trackSelectionParameters } returns trackParameters
    }

    private companion object {
        const val POSITION_MS = 810_675L
        const val VOLUME = 0.6f
        val SPEED = PlaybackParameters(1.25f)
    }
}
