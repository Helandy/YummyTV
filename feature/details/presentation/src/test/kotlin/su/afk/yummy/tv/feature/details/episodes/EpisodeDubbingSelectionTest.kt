package su.afk.yummy.tv.feature.details.episodes

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.resolveDetailsPlayerSelection
import su.afk.yummy.tv.feature.details.episodes.dubbings.selectEpisodeDubbingLaunchVideo
import su.afk.yummy.tv.feature.details.mapper.episodeDubbingItems

/**
 * Регресс на «Пиратов Чёрной лагуны»: yani отдаёт вторую серию и как `"02"` (Kodik),
 * и как `"2"` (остальные балансеры). Точное сравнение строк схлопывало выбор до одного
 * плеера и запрещало смену озвучки.
 */
class EpisodeDubbingSelectionTest : BaseUnitTest() {

    private val videos = listOf(
        video(id = 1, episode = "02", dubbing = MC, player = "Плеер Kodik", views = 5392),
        video(id = 2, episode = "2", dubbing = MC, player = "Плеер Kodik", views = 94),
        video(id = 3, episode = "2", dubbing = MC, player = "Плеер CVH", views = 208),
        video(id = 4, episode = "2", dubbing = MC, player = "Плеер Sibnet", views = 12),
        video(id = 5, episode = "2", dubbing = SHIZA, player = "Плеер Kodik", views = 255),
        video(id = 6, episode = "3", dubbing = LAMP, player = "Плеер Kodik", views = 1),
    )

    @Test
    fun `dubbings of an episode are found regardless of the leading zero`() {
        val names = videos.episodeDubbingItems("02").map { it.name }

        assertEquals(listOf(MC, SHIZA), names)
    }

    @Test
    fun `balancer label counts only players that have this very episode`() {
        val items = videos.episodeDubbingItems("2").associateBy { it.name }

        assertEquals("CVH • Kodik • Sibnet", items.getValue(MC).supportedBalancers)
        assertEquals("Kodik", items.getValue(SHIZA).supportedBalancers)
    }

    @Test
    fun `dubbing without this episode is not offered`() {
        assertEquals(
            emptyList<String>(),
            videos.episodeDubbingItems("2").map { it.name } - setOf(MC, SHIZA),
        )
    }

    @Test
    fun `balancer picker offers every player of the episode`() {
        val dubbingVideos = videos.filter { it.dubbing == MC }
        val candidate = dubbingVideos.selectEpisodeDubbingLaunchVideo(
            episode = "02",
            dubbingName = MC,
            preferredPlayer = PreferredPlayer.NONE,
        )

        val selection = resolveDetailsPlayerSelection(
            video = requireNotNull(candidate),
            allVideos = dubbingVideos,
            preferredPlayer = PreferredPlayer.NONE,
        )

        val picker = (selection as DetailsPlayerSelection.ShowPicker).picker
        assertEquals(
            listOf("Плеер CVH", "Плеер Kodik", "Плеер Sibnet"),
            picker.options.map { it.playerName }.sorted(),
        )
    }

    private fun video(
        id: Int,
        episode: String,
        dubbing: String,
        player: String,
        views: Int,
    ) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = player,
        playerId = null,
        iframeUrl = "//kodikplayer.com/serial/11971/hash/720p?episode=$id",
        durationSeconds = 1400,
        views = views,
    )

    private companion object {
        const val MC = "Озвучка MC Entertainment"
        const val SHIZA = "Озвучка SHIZA Project"
        const val LAMP = "Озвучка LampStudio"
    }
}
