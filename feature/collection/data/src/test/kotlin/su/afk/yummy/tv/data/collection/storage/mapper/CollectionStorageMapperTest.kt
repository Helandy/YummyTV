package su.afk.yummy.tv.data.collection.storage.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.collection.dto.YaniCollectionAnimeDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionPosterDto

class CollectionStorageMapperTest : BaseUnitTest() {

    @Test
    fun `catalog keeps first three preview posters`() {
        val summary = listOf(collection(previews = listOf("a", "b", "c", "d")))
            .toSummary()

        assertEquals(listOf("https://a", "https://b", "https://c"), summary.posterUrls)
    }

    @Test
    fun `catalog tops up missing previews with anime posters`() {
        val summary = listOf(collection(previews = listOf("a"), animePosters = listOf("a", "x", "y", "z")))
            .toSummary()

        assertEquals(listOf("https://a", "https://x", "https://y"), summary.posterUrls)
    }

    @Test
    fun `catalog without posters has empty list`() {
        val summary = listOf(collection()).toSummary()

        assertEquals(emptyList<String>(), summary.posterUrls)
    }

    private fun List<YaniCollectionDetailDto>.toSummary() =
        toCollectionCatalogPageCache(
            pageKey = "key",
            language = "ru",
            limit = 20,
            offset = 0,
            responseSize = size,
            cachedAt = 0L,
        ).toCollectionSummaryPage().items.single()

    private fun collection(
        previews: List<String> = emptyList(),
        animePosters: List<String> = emptyList(),
    ) = YaniCollectionDetailDto(
        id = 1,
        title = "Collection",
        posterPreviews = previews.map { YaniCollectionPosterDto(big = "//$it") },
        animes = animePosters.mapIndexed { index, url ->
            YaniCollectionAnimeDto(animeId = index, poster = YaniCollectionPosterDto(big = "//$url"))
        },
    )
}
