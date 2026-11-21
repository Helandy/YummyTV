package su.afk.yummy.tv.feature.player.mapper

import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.utils.isSyntheticMasterUrl

class PlayerPlaybackUiStateMapperTest : BaseUnitTest() {

    @Test
    fun `ошибка из-за сети не называет виноватым плеер`() {
        val state = PlayerState.State(playerError = NO_CONNECTION)

        val ui = state.toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        assertNull(ui.errorBalancerLabel)
        org.junit.Assert.assertFalse(ui.canChangePlayer)
        org.junit.Assert.assertFalse(ui.canChangeDubbing)
    }

    @Test
    fun `офлайн с несколькими скачанными сериями показывает ленту`() {
        val state = offlineState(episodeCount = 3)

        val ui = state.toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        org.junit.Assert.assertEquals(listOf("1", "2", "3"), ui.episodes.map { it.number })
        org.junit.Assert.assertTrue(ui.episodes.all { it.available })
    }

    @Test
    fun `офлайн с одной скачанной серией ленту не показывает`() {
        val ui = offlineState(episodeCount = 1)
            .toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        org.junit.Assert.assertTrue(ui.episodes.isEmpty())
    }

    private val kodikQualities = linkedMapOf(
        "360p" to "https://cdn.test/v/360.mp4:hls:manifest.m3u8",
        "720p" to "https://cdn.test/v/720.mp4:hls:manifest.m3u8",
    )

    @Test
    fun `адаптивный режим играет синтетический master с потолком по выбранному качеству`() {
        val state = PlayerState.State(
            adaptiveQualityEnabled = true,
            streamUrl = kodikQualities.getValue("720p"),
            streamQualityMap = kodikQualities,
            selectedQuality = "720p",
        )

        val ui = state.toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        org.junit.Assert.assertTrue(ui.playbackUrl.isSyntheticMasterUrl())
        org.junit.Assert.assertEquals(720, ui.videoHeightCap)
        org.junit.Assert.assertFalse(ui.videoHeightPinned)
    }

    @Test
    fun `источник без отдельных HLS-плейлистов играет ссылку выбранного качества`() {
        val mp4 = linkedMapOf("360p" to "https://cdn.test/a.mp4", "720p" to "https://cdn.test/b.mp4")
        val state = PlayerState.State(
            streamUrl = mp4.getValue("720p"),
            streamQualityMap = mp4,
            selectedQuality = "360p",
        )

        val ui = state.toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        org.junit.Assert.assertEquals("https://cdn.test/a.mp4", ui.playbackUrl)
        assertNull(ui.videoHeightCap)
    }

    @Test
    fun `ручной выбор качества закрепляет вариант master без пересоздания потока`() {
        val state = PlayerState.State(
            adaptiveQualityEnabled = false,
            streamUrl = kodikQualities.getValue("720p"),
            streamQualityMap = kodikQualities,
            selectedQuality = "360p",
        )

        val ui = state.toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        org.junit.Assert.assertTrue(ui.playbackUrl.isSyntheticMasterUrl())
        org.junit.Assert.assertEquals(360, ui.videoHeightCap)
        org.junit.Assert.assertTrue(ui.videoHeightPinned)
    }

    private fun offlineState(episodeCount: Int) = PlayerState.State(
        animeId = 1,
        isOfflinePlayback = true,
        sourceGraph = PlayerSourceGraph(
            balancers = listOf(
                PlayerSourceBalancer(
                    name = "kodik",
                    dubbings = listOf(
                        PlayerSourceDubbing(
                            name = "AniLibria",
                            episodes = (1..episodeCount).map {
                                PlayerSourceEpisode(id = it, number = it.toString(), downloadId = it.toLong())
                            },
                        ),
                    ),
                ),
            ),
        ),
        sourceSelection = PlayerSourceSelection(episodeIndex = 0),
    )

    private companion object {
        const val NO_CONNECTION = "Нет подключения к интернету"
    }
}
