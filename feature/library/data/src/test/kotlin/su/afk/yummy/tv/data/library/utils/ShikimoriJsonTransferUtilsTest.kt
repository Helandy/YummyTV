package su.afk.yummy.tv.data.library.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.utils.formatMalXmlAnimeList
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ShikimoriJsonTransferUtilsTest : BaseUnitTest() {

    @Test
    fun `shikimori json export uses exact canonical schema and round trips all statuses`() {
        val entries = listOf(
            ExternalListEntry(52991, "Sousou no Frieren", UserAnimeList.COMPLETED, score = 10, watchedEpisodes = 28, rewatches = 1),
            ExternalListEntry(21, "One Piece", UserAnimeList.WATCHING, score = 9, watchedEpisodes = 1100),
            ExternalListEntry(9253, "Steins;Gate", UserAnimeList.PLANNED, score = null, watchedEpisodes = 0),
            ExternalListEntry(1535, "Death Note", UserAnimeList.POSTPONED, score = 8, watchedEpisodes = 12),
            ExternalListEntry(20, "Naruto", UserAnimeList.DROPPED, score = 5, watchedEpisodes = 30),
        )

        val json = formatShikimoriJsonAnimeList(entries)
        val firstObject = Json.parseToJsonElement(json).jsonArray.first().jsonObject
        assertEquals(
            setOf(
                "target_title",
                "target_id",
                "target_type",
                "score",
                "status",
                "rewatches",
                "episodes",
                "text",
            ),
            firstObject.keys,
        )

        val parsed = parseShikimoriJsonAnimeList(json)
        assertEquals(ExternalListFormat.SHIKIMORI_JSON, parsed.format)
        assertEquals(5, parsed.entries.size)
        assertEquals(0, parsed.skippedNonAnimeCount)
        assertEquals(0, parsed.skippedInvalidCount)

        val byId = parsed.entries.associateBy { it.externalId }
        assertEquals(UserAnimeList.COMPLETED, byId.getValue(52991).list)
        assertEquals(10, byId.getValue(52991).score)
        assertEquals(28, byId.getValue(52991).watchedEpisodes)
        assertEquals(UserAnimeList.WATCHING, byId.getValue(21).list)
        assertEquals(UserAnimeList.PLANNED, byId.getValue(9253).list)
        assertNull(byId.getValue(9253).score)
        assertEquals(UserAnimeList.POSTPONED, byId.getValue(1535).list)
        assertEquals(UserAnimeList.DROPPED, byId.getValue(20).list)
    }

    @Test
    fun `filters non anime targets handles rewatching and nested anime payload`() {
        val json = """
            [
              {
                "target_title": "Berserk",
                "target_id": 2,
                "target_type": "Manga",
                "score": 10,
                "status": "completed",
                "rewatches": 0,
                "episodes": 0,
                "text": null
              },
              {
                "target_title": "Code Geass",
                "target_title_ru": "Код Гиас",
                "target_id": 1575,
                "target_type": "Anime",
                "score": 10,
                "status": "rewatching",
                "rewatches": 2,
                "episodes": 25,
                "text": "Masterpiece"
              },
              {
                "score": 8,
                "status": "on_hold",
                "episodes": 6,
                "anime": {
                  "id": 30276,
                  "name": "One Punch Man",
                  "russian": "Ванпанчмен",
                  "kind": "tv",
                  "episodes": 12
                }
              }
            ]
        """.trimIndent()

        val parsed = parseShikimoriJsonAnimeList(json)
        assertEquals(2, parsed.entries.size)
        assertEquals(1, parsed.skippedNonAnimeCount)
        assertEquals("Код Гиас", parsed.entries[0].title)
        assertEquals(UserAnimeList.WATCHING, parsed.entries[0].list)
        assertEquals(30276, parsed.entries[1].externalId)
        assertEquals("Ванпанчмен", parsed.entries[1].title)
        assertEquals(UserAnimeList.POSTPONED, parsed.entries[1].list)
    }

    @Test
    fun `parseExternalListBytes detects json xml and gzipped xml automatically`() {
        val sample = listOf(
            ExternalListEntry(1, "Cowboy Bebop", UserAnimeList.COMPLETED, score = 9, watchedEpisodes = 26),
        )

        val jsonBytes = formatShikimoriJsonAnimeList(sample).toByteArray(Charsets.UTF_8)
        assertEquals(ExternalListFormat.SHIKIMORI_JSON, parseExternalListBytes(jsonBytes).format)

        val xml = formatMalXmlAnimeList("user", sample)
        assertEquals(ExternalListFormat.MAL_XML, parseExternalListBytes(xml.toByteArray()).format)

        val gzBytes = ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).bufferedWriter(Charsets.UTF_8).use { it.write(xml) }
            out.toByteArray()
        }
        assertEquals(ExternalListFormat.MAL_XML, parseExternalListBytes(gzBytes).format)
    }
}
