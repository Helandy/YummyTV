package su.afk.yummy.tv.data.player.extractor.common

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class QualityMapTest : BaseUnitTest() {

    @Test
    fun `orders known keys first, leftovers appended in original order`() {
        val raw = linkedMapOf(
            "720p" to "url720",
            "auto" to "urlAuto",
            "unknown" to "urlUnknown",
            "144p" to "url144",
        )

        val ordered = orderQualityMap(raw)

        assertEquals(
            listOf("auto", "144p", "720p", "unknown"),
            ordered.keys.toList(),
        )
    }

    @Test
    fun `missing keys are simply skipped, not inserted as blanks`() {
        val raw = linkedMapOf("480p" to "url480")

        val ordered = orderQualityMap(raw)

        assertEquals(mapOf("480p" to "url480"), ordered)
    }

    @Test
    fun `custom key order is respected`() {
        val raw = linkedMapOf("q360" to "u360", "q720" to "u720", "q1080" to "u1080")

        val ordered = orderQualityMap(raw, keys = listOf("q1080", "q720", "q360"))

        assertEquals(listOf("q1080", "q720", "q360"), ordered.keys.toList())
    }

    @Test
    fun `keyAliases falls back to bare-digit key when the p-suffixed key is missing`() {
        // Mirrors VkExtractor's behaviour: a "720" entry (no "p") should still land under "720p".
        val raw = linkedMapOf("720" to "urlBare", "1080p" to "url1080")

        val ordered = orderQualityMap(
            raw = raw,
            keyAliases = { key ->
                if (key == "auto") {
                    listOf(key)
                } else {
                    listOf(
                        key,
                        key.removeSuffix("p"),
                    )
                }
            },
        )

        assertEquals("urlBare", ordered["720p"])
        assertEquals("url1080", ordered["1080p"])
    }

    @Test
    fun `withAutoQualityLabel renames only the auto entry`() {
        val map = linkedMapOf("auto" to "urlAuto", "720p" to "url720")

        val renamed = map.withAutoQualityLabel("Авто")

        assertEquals(linkedMapOf("Авто" to "urlAuto", "720p" to "url720"), renamed)
    }

    @Test
    fun `withAutoQualityLabel is a no-op for blank or literal auto label`() {
        val map = linkedMapOf("auto" to "urlAuto")

        assertEquals(map, map.withAutoQualityLabel(""))
        assertEquals(map, map.withAutoQualityLabel("auto"))
    }

    @Test
    fun `toResolveResult maps fields 1-1`() {
        val qualities = linkedMapOf("auto" to "url")
        val stream = ExtractedStream(
            url = "url",
            headers = mapOf("Referer" to "https://example.com"),
            qualities = qualities,
        )

        val result = stream.toResolveResult()

        assertEquals("url", result.url)
        assertEquals(mapOf("Referer" to "https://example.com"), result.headers)
        assertEquals(qualities, result.qualities)
    }
}
