package su.afk.yummy.tv.data.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.network.yani.YaniApiJson
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto

/**
 * Резервная копия YummyTV, пока в ней нет полных деталей, отдаёт минимальный объект из каталога. Он должен разбираться, а MAL id —
 * доходить до `AnimeDetails`: по нему деталка находит серии и дополнительные источники без yani.
 */
class YaniAnimeMinimalDetailsMapperTest : BaseUnitTest() {

    @Test
    fun `minimal copy response maps with mal id and without episodes`() {
        val json = """{"response":{"anime_id":42,"title":"Клинок","description":"","year":2019,"season":2,
            "type":{"shortname":"tv"},"remote_ids":{"myanimelist_id":38000},
            "poster":{"small":"//static.yani.tv/posters/small/1.jpg","mega":"//static.yani.tv/posters/mega/1.jpg"}}}"""

        val details = YaniApiJson.decodeFromString<YaniAnimeDetailsDto>(json).toAnimeDetails()

        assertEquals(42, details.id)
        assertEquals(38000, details.malId)
        assertEquals(2019, details.year)
        assertNull(details.episodes)
        assertNotNull(details.poster)
        assertEquals(emptyList<Any>(), details.genres)
    }
}
