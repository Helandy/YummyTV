package su.afk.yummy.tv.domain.watching.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest

/** Выбор видео под явный запрос диплинка: серия, балансер и озвучка. */
class RequestedPlaybackUtilsTest : BaseUnitTest() {

    private val allohaEp4 = video(1, "4", "AniLibria", "Alloha", "https://alloha.yani.tv/?token=1")
    private val allohaEp04Mir = video(2, "04", "Studio Mir", "Alloha", "https://alloha.yani.tv/?token=2")
    private val kodikEp4 = video(3, "4", "AniLibria", "Kodik", "https://kodik.info/seria/3/hash/720p")
    private val allohaEp5 = video(4, "5", "AniLibria", "Alloha", "https://alloha.yani.tv/?token=4")
    private val videos = listOf(kodikEp4, allohaEp4, allohaEp04Mir, allohaEp5)

    @Test
    fun `balancer and episode pick the matching video`() {
        assertEquals(allohaEp4, videos.selectRequestedVideo(episode = "4", player = "alloha", dubbing = null))
    }

    @Test
    fun `balancer match ignores case`() {
        assertEquals(kodikEp4, videos.selectRequestedVideo(episode = "4", player = "KODIK", dubbing = null))
    }

    @Test
    fun `episode numbers with a leading zero count as the same episode`() {
        assertEquals(
            allohaEp04Mir,
            videos.selectRequestedVideo(episode = "4", player = "alloha", dubbing = "mir"),
        )
    }

    @Test
    fun `dubbing narrows the choice`() {
        assertEquals(
            allohaEp5,
            videos.selectRequestedVideo(episode = "5", player = null, dubbing = "AniLibria"),
        )
    }

    @Test
    fun `no conditions picks the first playable video`() {
        assertEquals(kodikEp4, videos.selectRequestedVideo(episode = null, player = null, dubbing = null))
    }

    @Test
    fun `blank conditions do not restrict the choice`() {
        assertEquals(kodikEp4, videos.selectRequestedVideo(episode = " ", player = "", dubbing = " "))
    }

    @Test
    fun `nothing matches returns null`() {
        assertNull(videos.selectRequestedVideo(episode = "9", player = "alloha", dubbing = null))
        assertNull(videos.selectRequestedVideo(episode = "4", player = "nothing", dubbing = null))
    }

    @Test
    fun `a video of an unsupported player is never picked`() {
        val unknown = video(9, "4", "Dub", "Unknown", "https://example.com/video")
        assertNull(listOf(unknown).selectRequestedVideo(episode = "4", player = null, dubbing = null))
    }

    private fun video(id: Int, episode: String, dubbing: String, player: String, iframeUrl: String) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = player,
        playerId = null,
        iframeUrl = iframeUrl,
        durationSeconds = null,
    )
}
