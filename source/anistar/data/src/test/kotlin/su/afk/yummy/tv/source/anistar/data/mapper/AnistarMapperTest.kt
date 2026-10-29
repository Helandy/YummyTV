package su.afk.yummy.tv.source.anistar.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant

class AnistarMapperTest {
    private fun availability(
        key: String = "2658",
        url: String = "https://v31.astar.bz/test/player2/videoas_p2p_new.php?id=2658",
        numbers: List<Int> = listOf(2, 1, 0),
    ) = SourceAvailability(
        sourceId = "anistar", key = key, url = url, episodesAvailable = numbers.size, episodesTotal = 12, ongoing = true, qualities = emptyList(),
        variants = listOf(SourceVariant(key, isDubbing = false, episodes = numbers.map { SourceEpisode(it, null, null, null) })),
    )

    @Test
    fun `title takes the news id from the key the mirror from the page host and positive episodes from the variant`() {
        val title = AnistarMapper.toTitle(availability())

        assertEquals(AnistarTitle(2658, "https://v31.astar.bz", listOf(AnistarEpisode(2), AnistarEpisode(1))), title)
    }

    @Test
    fun `answer without a usable id or https page gives no title`() {
        assertNull(AnistarMapper.toTitle(availability(key = "x")))
        assertNull(AnistarMapper.toTitle(availability(key = "0")))
        assertNull(AnistarMapper.toTitle(availability(url = "http://v31.astar.bz/p?id=1")))
        assertNull(AnistarMapper.toTitle(availability(url = "not a url")))
    }
}
