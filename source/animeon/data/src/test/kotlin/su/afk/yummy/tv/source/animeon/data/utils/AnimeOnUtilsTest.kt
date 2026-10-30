package su.afk.yummy.tv.source.animeon.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget

class AnimeOnUtilsTest {

    @Test
    fun `episode id is taken only when the page starts with the requested episode`() {
        val body = """{"episodes":[{"id":14211,"poster":"","episode":5,"sourceEpisode":5}],"pagination":{"total":28}}"""

        assertEquals(14211, animeOnEpisodeId(body, 5))
        assertNull(animeOnEpisodeId(body, 6))
        assertNull(animeOnEpisodeId("""{"episodes":[]}""", 1))
    }

    @Test
    fun `player url comes from the video url field`() {
        assertEquals(
            "https://ashdi.vip/vod/104245?player=animeon.club",
            animeOnPlayerUrl("""{"videoUrl":"https://ashdi.vip/vod/104245?player=animeon.club"}"""),
        )
        assertNull(animeOnPlayerUrl("{}"))
        assertNull(animeOnPlayerUrl("""{"videoUrl":""}"""))
    }

    @Test
    fun `source url round trips and rejects foreign shapes`() {
        val target = AnimeOnTarget("924", translationId = 1093, playerId = 1209, episode = 3)

        assertEquals("source://animeon/924/3/1093/1209", target.toSourceUrl())
        assertEquals(target, parseAnimeOnSourceUrl("source://animeon/924/3/1093/1209"))
        assertNull(parseAnimeOnSourceUrl("source://animeon/924/3/1093"))
        assertNull(parseAnimeOnSourceUrl("source://animeon/abc/3/1093/1209"))
        assertNull(parseAnimeOnSourceUrl("source://animeon/924/0/1093/1209"))
        assertNull(parseAnimeOnSourceUrl("source://anidub/924/3/1093/1209"))
    }

    @Test
    fun `moon pages are recognized on every mirror, other players are not`() {
        assertEquals(true, isMoonPlayerUrl("https://moonanime.art/iframe/abcd1234/?player=animeon.club&api=1"))
        assertEquals(true, isMoonPlayerUrl("https://moonanime.net/iframe/28e91vdh5vgo/?player=animeon.club&api=1"))
        assertEquals(false, isMoonPlayerUrl("https://ashdi.vip/vod/104245?player=animeon.club"))
        assertEquals(false, isMoonPlayerUrl("https://moonanime.art.evil.example/iframe/abcd1234/"))
    }
}
