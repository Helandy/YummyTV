package su.afk.yummy.tv.source.animeon.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.animeon.data.network.AnimeOnApi
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreams
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreamsResult
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreams
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import su.afk.yummy.tv.source.ashdi.domain.repository.AshdiRepository
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreams
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult
import su.afk.yummy.tv.source.fenix.domain.repository.FenixRepository
import su.afk.yummy.tv.source.moon.domain.model.MoonStreams
import su.afk.yummy.tv.source.moon.domain.model.MoonStreamsResult
import su.afk.yummy.tv.source.moon.domain.repository.MoonRepository

class AnimeOnRepositoryImplTest : BaseUnitTest() {

    private val api: AnimeOnApi = mockk()
    private val ashdi: AshdiRepository = mockk()
    private val fenix: FenixRepository = mockk()
    private val moon: MoonRepository = mockk()
    private val target = AnimeOnTarget("924", translationId = 1093, playerId = 1209, episode = 1)
    private val playerUrl = "https://ashdi.vip/vod/104245?player=animeon.club"

    private fun createRepository() = AnimeOnRepositoryImpl(api, ashdi, fenix, moon)

    private fun givenSiteChain() {
        coEvery {
            api.getText("https://animeon.club/api/player/924/episodes?take=1&skip=0&playerId=1209&translationId=1093")
        } returns """{"episodes":[{"id":14207,"episode":1}]}"""
        coEvery { api.getText("https://animeon.club/api/player/14207/episode") } returns """{"videoUrl":"$playerUrl"}"""
    }

    @Test
    fun `site chain hands the player page to ashdi`() = runTest {
        givenSiteChain()
        coEvery { ashdi.getStreams(playerUrl) } returns
            AshdiStreamsResult.Success(AshdiStreams(mapOf("720p" to "https://a/720.m3u8"), "https://a/index.m3u8"))

        assertEquals(
            AnimeOnStreamsResult.Success(AnimeOnStreams("https://a/index.m3u8", mapOf("720p" to "https://a/720.m3u8"))),
            createRepository().getStreams(target),
        )
    }

    @Test
    fun `ashdi region block is passed through`() = runTest {
        givenSiteChain()
        coEvery { ashdi.getStreams(playerUrl) } returns AshdiStreamsResult.RegionBlocked

        assertEquals(AnimeOnStreamsResult.RegionBlocked, createRepository().getStreams(target))
    }

    @Test
    fun `missing episode gives not found`() = runTest {
        coEvery { api.getText(any()) } returns null

        assertEquals(AnimeOnStreamsResult.NotFound, createRepository().getStreams(target))
    }

    @Test
    fun `fenix page goes to fenix with the track the site selected and plays the master adaptively`() = runTest {
        val embed = "https://fenixplay.xyz/embed/abc.def?ep=1&edition=tv"
        coEvery {
            api.getText("https://animeon.club/api/player/924/episodes?take=1&skip=0&playerId=1209&translationId=1093")
        } returns """{"episodes":[{"id":14207,"episode":1}]}"""
        coEvery { api.getText("https://animeon.club/api/player/14207/episode") } returns
            """{"videoUrl":"$embed","selection":{"episode":1,"edition":"tv","track":"FanVoxUa"}}"""
        coEvery { fenix.getStreams(embed, "FanVoxUa") } returns
            FenixStreamsResult.Success(FenixStreams("https://media.fenixplay.xyz/m/master.m3u8?token=t"))

        val result = createRepository().getStreams(target) as AnimeOnStreamsResult.Success

        assertEquals("https://media.fenixplay.xyz/m/master.m3u8?token=t", result.streams.masterUrl)
        assertEquals(emptyMap<String, String>(), result.streams.qualities)
    }

    @Test
    fun `fenix not found and failure are passed through`() = runTest {
        val embed = "https://fenixplay.xyz/embed/abc.def?ep=1"
        coEvery { api.getText(any()) } returnsMany listOf(
            """{"episodes":[{"id":14207,"episode":1}]}""",
            """{"videoUrl":"$embed"}""",
            """{"episodes":[{"id":14207,"episode":1}]}""",
            """{"videoUrl":"$embed"}""",
        )
        coEvery { fenix.getStreams(embed, null) } returnsMany listOf(FenixStreamsResult.NotFound, FenixStreamsResult.Failed("x"))

        assertEquals(AnimeOnStreamsResult.NotFound, createRepository().getStreams(target))
        assertEquals(AnimeOnStreamsResult.Failed("x"), createRepository().getStreams(target))
    }

    @Test
    fun `moon page goes to moon and keeps the headers of the stream`() = runTest {
        val page = "https://moonanime.art/iframe/abcd1234/?player=animeon.club&api=1"
        coEvery {
            api.getText("https://animeon.club/api/player/924/episodes?take=1&skip=0&playerId=1209&translationId=1093")
        } returns """{"episodes":[{"id":14207,"episode":1}]}"""
        coEvery { api.getText("https://animeon.club/api/player/14207/episode") } returns """{"videoUrl":"$page"}"""
        coEvery { moon.getStreams(page) } returns
            MoonStreamsResult.Success(MoonStreams("https://s.example/v.m3u8", mapOf("Referer" to "https://moonanime.art/iframe/abcd1234/")))

        val result = createRepository().getStreams(target) as AnimeOnStreamsResult.Success

        assertEquals("https://s.example/v.m3u8", result.streams.masterUrl)
        assertEquals(mapOf("Referer" to "https://moonanime.art/iframe/abcd1234/"), result.streams.headers)
        assertEquals(emptyMap<String, String>(), result.streams.qualities)
    }

    @Test
    fun `moon not found and failure are passed through`() = runTest {
        val page = "https://moonanime.art/iframe/abcd1234/"
        coEvery { api.getText(any()) } returnsMany listOf(
            """{"episodes":[{"id":14207,"episode":1}]}""",
            """{"videoUrl":"$page"}""",
            """{"episodes":[{"id":14207,"episode":1}]}""",
            """{"videoUrl":"$page"}""",
        )
        coEvery { moon.getStreams(page) } returnsMany listOf(MoonStreamsResult.NotFound, MoonStreamsResult.Failed("x"))

        assertEquals(AnimeOnStreamsResult.NotFound, createRepository().getStreams(target))
        assertEquals(AnimeOnStreamsResult.Failed("x"), createRepository().getStreams(target))
    }
}
