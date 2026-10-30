package su.afk.yummy.tv.data.details.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimeTypeKind
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem

class FranchiseEpisodeOffsetUtilsTest {

    private fun item(id: Int, kind: AnimeTypeKind) = AnimeViewingOrderItem(
        animeId = id,
        title = "t$id",
        relation = null,
        type = null,
        typeKind = kind,
        episodesCount = null,
        poster = null,
        year = null,
        rating = null,
    )

    private fun episodes(count: Int?, aired: Int?) =
        AnimeEpisodes(count = count, aired = aired, nextDateEpochSeconds = 0L, prevDateEpochSeconds = 0L)

    private val order = listOf(
        item(1, AnimeTypeKind.TV),
        item(2, AnimeTypeKind.OVA),
        item(3, AnimeTypeKind.TV),
        item(4, AnimeTypeKind.TV),
    )

    @Test
    fun `previous TV entries skip other kinds and later entries`() {
        assertEquals(listOf(1, 3), order.previousTvEntryIds(4))
    }

    @Test
    fun `first entry and unknown entry have no predecessors`() {
        assertEquals(emptyList<Int>(), order.previousTvEntryIds(1))
        assertEquals(emptyList<Int>(), order.previousTvEntryIds(99))
    }

    @Test
    fun `count falls back to aired`() {
        assertEquals(24, episodes(24, 10).totalOrAiredCount())
        assertEquals(10, episodes(null, 10).totalOrAiredCount())
        assertNull(episodes(null, null).totalOrAiredCount())
        assertNull((null as AnimeEpisodes?).totalOrAiredCount())
    }
}
