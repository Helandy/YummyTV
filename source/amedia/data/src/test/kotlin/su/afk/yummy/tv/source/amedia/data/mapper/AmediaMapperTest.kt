package su.afk.yummy.tv.source.amedia.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisodeSource
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant

class AmediaMapperTest : BaseUnitTest() {

    private fun availability(url: String = "https://amd.online/1-x.html", vararg episodes: SourceEpisode) = SourceAvailability(
        sourceId = "amedia", key = "1-x", url = url, episodesAvailable = episodes.size, episodesTotal = 12, ongoing = true,
        qualities = emptyList(), variants = listOf(SourceVariant("1", false, episodes.toList())),
    )

    private fun episode(number: Int, id: String? = null, url: String? = null) = SourceEpisode(number, null, null, null, id, url)

    @Test
    fun `only an amd online page becomes a title`() {
        assertEquals(AmediaTitle("https://amd.online/1-x.html", emptyList()), AmediaMapper.toTitle(availability(" https://amd.online/1-x.html ")))
        assertNull(AmediaMapper.toTitle(availability("https://evil.example/1-x.html")))
    }

    @Test
    fun `episodes become vod and rutube sources and malformed ones are dropped`() {
        val embed = "https://rutube.ru/play/embed/be753fcea5b1c586363cf8a220c47167"
        val title = AmediaMapper.toTitle(
            availability(
                episodes = arrayOf(
                    episode(1, id = "47997"),
                    episode(2, url = embed),
                    episode(3, url = "https://evil.example/play/embed/abc"),
                    episode(4),
                    episode(0, id = "1"),
                    episode(5, id = "abc"),
                    episode(6, id = "-3"),
                ),
            ),
        )

        assertEquals(
            listOf(AmediaEpisode(1, AmediaEpisodeSource.Vod(47997)), AmediaEpisode(2, AmediaEpisodeSource.Rutube(embed))),
            title!!.episodes,
        )
    }

    @Test
    fun `availability without variants gives a title without episodes`() {
        val noVariants = availability().copy(variants = emptyList())

        assertEquals(emptyList<AmediaEpisode>(), AmediaMapper.toTitle(noVariants)!!.episodes)
    }
}
