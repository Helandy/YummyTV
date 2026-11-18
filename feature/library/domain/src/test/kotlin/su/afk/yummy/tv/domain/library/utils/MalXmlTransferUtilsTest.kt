package su.afk.yummy.tv.domain.library.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ExternalListParseError
import su.afk.yummy.tv.domain.library.model.ExternalListParseException
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class MalXmlTransferUtilsTest : BaseUnitTest() {

    @Test
    fun `mal xml round trip preserves all statuses ratings and update flag`() {
        val original = listOf(
            ExternalListEntry(
                externalId = 52991,
                title = "Sousou no Frieren",
                list = UserAnimeList.COMPLETED,
                score = 10,
                watchedEpisodes = 28,
                rewatches = 1,
                seriesType = "TV",
            ),
            ExternalListEntry(
                externalId = 21,
                title = "One Piece",
                list = UserAnimeList.WATCHING,
                score = 9,
                watchedEpisodes = 1100,
                seriesType = "TV",
            ),
            ExternalListEntry(
                externalId = 9253,
                title = "Steins;Gate ]]> Special & <OVA>",
                list = UserAnimeList.PLANNED,
                score = null,
                watchedEpisodes = 0,
                seriesType = "OVA",
            ),
            ExternalListEntry(
                externalId = 1535,
                title = "Death Note",
                list = UserAnimeList.POSTPONED,
                score = 8,
                watchedEpisodes = 15,
            ),
            ExternalListEntry(
                externalId = 20,
                title = "Naruto",
                list = UserAnimeList.DROPPED,
                score = 6,
                watchedEpisodes = 40,
            ),
        )

        val xml = formatMalXmlAnimeList(userName = "tester", entries = original)
        assertTrue(xml.contains("<user_export_type>1</user_export_type>"))
        assertTrue(xml.contains("<update_on_import>1</update_on_import>"))
        assertTrue(xml.contains("<user_total_anime>5</user_total_anime>"))
        assertTrue(xml.contains("<user_total_plantowatch>1</user_total_plantowatch>"))

        val parsed = parseMalXmlAnimeList(xml)
        assertEquals(ExternalListFormat.MAL_XML, parsed.format)
        assertEquals(5, parsed.entries.size)
        assertEquals(0, parsed.skippedInvalidCount)

        val byId = parsed.entries.associateBy { it.externalId }
        assertEquals(UserAnimeList.COMPLETED, byId.getValue(52991).list)
        assertEquals(10, byId.getValue(52991).score)
        assertEquals(28, byId.getValue(52991).watchedEpisodes)
        assertEquals(1, byId.getValue(52991).rewatches)

        assertEquals(UserAnimeList.WATCHING, byId.getValue(21).list)
        assertEquals(UserAnimeList.PLANNED, byId.getValue(9253).list)
        assertNull(byId.getValue(9253).score)
        assertEquals("Steins;Gate ]]> Special & <OVA>", byId.getValue(9253).title)

        assertEquals(UserAnimeList.POSTPONED, byId.getValue(1535).list)
        assertEquals(UserAnimeList.DROPPED, byId.getValue(20).list)
    }

    @Test
    fun `parses numeric mal status codes and shikimori xml status fallback`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <myanimelist>
                <anime>
                    <series_animedb_id>1</series_animedb_id>
                    <series_title>Cowboy Bebop</series_title>
                    <my_status>6</my_status>
                    <my_score>0</my_score>
                </anime>
                <anime>
                    <series_animedb_id>5114</series_animedb_id>
                    <series_title>Fullmetal Alchemist: Brotherhood</series_title>
                    <shiki_status>rewatching</shiki_status>
                    <my_score>10</my_score>
                    <my_watched_episodes>12</my_watched_episodes>
                </anime>
                <anime>
                    <series_animedb_id>0</series_animedb_id>
                    <my_status>Completed</my_status>
                </anime>
            </myanimelist>
        """.trimIndent()

        val parsed = parseMalXmlAnimeList(xml)
        assertEquals(2, parsed.entries.size)
        assertEquals(1, parsed.skippedInvalidCount)
        assertEquals(UserAnimeList.PLANNED, parsed.entries[0].list)
        assertEquals(UserAnimeList.WATCHING, parsed.entries[1].list)
        assertEquals(10, parsed.entries[1].score)
    }

    @Test
    fun `decodes gzipped mal xml export and enforces decompressed size cap`() {
        val xml = formatMalXmlAnimeList(
            userName = "gzip_user",
            entries = listOf(
                ExternalListEntry(
                    externalId = 100,
                    title = "Gzip Anime",
                    list = UserAnimeList.COMPLETED,
                    score = 7,
                    watchedEpisodes = 12,
                ),
            ),
        )
        val compressed = ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).bufferedWriter(Charsets.UTF_8).use { it.write(xml) }
            out.toByteArray()
        }

        val decoded = compressed.decodeExternalListText()
        assertEquals(ExternalListFormat.MAL_XML, detectExternalListFormat(decoded))
        assertEquals(100, parseMalXmlAnimeList(decoded).entries.single().externalId)

        try {
            compressed.decodeExternalListText(maxDecompressedBytes = 32)
            fail("Expected FILE_TOO_LARGE")
        } catch (error: ExternalListParseException) {
            assertEquals(ExternalListParseError.FILE_TOO_LARGE, error.error)
        }
    }

    @Test
    fun `rejects malformed xml and unsupported files with typed parse errors`() {
        assertParseError(ExternalListParseError.MALFORMED_CONTENT) {
            parseMalXmlAnimeList("<myanimelist><anime><series_animedb_id>1")
        }
        assertParseError(ExternalListParseError.UNSUPPORTED_FORMAT) {
            detectExternalListFormat("plain text file")
        }
        assertParseError(ExternalListParseError.NO_VALID_ENTRIES) {
            parseMalXmlAnimeList("<myanimelist><manga><manga_mangadb_id>1</manga_mangadb_id></manga></myanimelist>")
        }
    }

    private fun assertParseError(expected: ExternalListParseError, block: () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (error: ExternalListParseException) {
            assertEquals(expected, error.error)
        }
    }
}
