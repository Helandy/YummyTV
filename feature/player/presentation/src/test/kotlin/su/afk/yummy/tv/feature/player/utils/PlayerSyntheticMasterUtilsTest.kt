package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PlayerSyntheticMasterUtilsTest : BaseUnitTest() {

    private fun kodik(vararg heights: Int) =
        heights.associateTo(LinkedHashMap()) { "${it}p" to "https://cdn.test/v/$it.mp4:hls:manifest.m3u8" }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decode(url: String) = String(Base64.Default.decode(url.removePrefix(SYNTHETIC_MASTER_URI_PREFIX)))

    @Test
    fun `master lists variants from low to high with absolute urls and resolution`() {
        val master = buildSyntheticHlsMasterUrl(kodik(720, 360, 480))!!

        assertTrue(master.isSyntheticMasterUrl())
        assertEquals(
            """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
            https://cdn.test/v/360.mp4:hls:manifest.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1200000,RESOLUTION=854x480
            https://cdn.test/v/480.mp4:hls:manifest.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
            https://cdn.test/v/720.mp4:hls:manifest.m3u8
            """.trimIndent() + "\n",
            decode(master),
        )
    }

    @Test
    fun `data part has no commas so Media3 can split it`() {
        val master = buildSyntheticHlsMasterUrl(kodik(360, 720))!!

        assertEquals(1, master.count { it == ',' })
    }

    @Test
    fun `anilibria per quality playlists are supported`() {
        val libria = linkedMapOf(
            "480p" to "https://cache.libria.fun/videos/media/ts/9000/1/480/8a7f4d218433f5a5fee1c6f5a02d278e.m3u8?countryIso=SC",
            "1080p" to "https://cache.libria.fun/videos/media/ts/9000/1/1080/7f3c1729ebd24b93d4e0918510004606.m3u8?countryIso=SC",
        )

        assertTrue(buildSyntheticHlsMasterUrl(libria)!!.isSyntheticMasterUrl())
    }

    @Test
    fun `ailiberty sameband and studiomir per quality playlists are supported`() {
        val ailiberty = linkedMapOf(
            "480p" to "https://ailiberty.top/video/playlists/eyJpdiI6.m3u8?expires=1&signature=a",
            "720p" to "https://ailiberty.top/video/playlists/eyJpdiI7.m3u8?expires=1&signature=b",
        )
        val sameband = linkedMapOf(
            "480p" to "https://sameband.studio/v/anime/BUBBLE/BUBBLE_RUS_2/index.m3u8",
            "1080p" to "https://sameband.studio/v/anime/BUBBLE/BUBBLE_RUS_0/index.m3u8",
        )
        val studiomir = linkedMapOf(
            "720p" to "https://hokage-alt.studiomir.club/animes/vids/encoded/107/1/hls/720/5a5f_720.m3u8",
            "1080p" to "https://hokage-alt.studiomir.club/animes/vids/encoded/107/1/hls/1080/5a5f_1080.m3u8",
        )

        listOf(ailiberty, sameband, studiomir).forEach { qualities ->
            assertTrue(buildSyntheticHlsMasterUrl(qualities)!!.isSyntheticMasterUrl())
        }
    }

    @Test
    fun `anistar per quality playlists are supported`() {
        val anistar = linkedMapOf(
            "360p" to "https://sf2.an-media.org/video/343d9aa1f90fa19228d189a1d92d19b7/360.mp4/index.m3u8",
            "720p" to "https://sf2.an-media.org/video/343d9aa1f90fa19228d189a1d92d19b7/720.mp4/index.m3u8",
        )

        assertTrue(buildSyntheticHlsMasterUrl(anistar)!!.isSyntheticMasterUrl())
    }

    @Test
    fun `a single quality does not need a master`() {
        assertNull(buildSyntheticHlsMasterUrl(kodik(720)))
    }

    @Test
    fun `sources that are not per quality hls are left alone`() {
        val mp4 = linkedMapOf("480p" to "https://cdn.test/a.mp4", "720p" to "https://cdn.test/b.mp4")

        assertNull(buildSyntheticHlsMasterUrl(mp4))
    }

    @Test
    fun `mixed map is not turned into a master`() {
        val mixed = kodik(480, 720).apply { put("1080p", "https://cdn.test/c.mp4") }

        assertNull(buildSyntheticHlsMasterUrl(mixed))
    }

    @Test
    fun `top variant is the best quality`() {
        val master = buildSyntheticHlsMasterUrl(kodik(360, 1080, 720))!!

        assertEquals("https://cdn.test/v/1080.mp4:hls:manifest.m3u8", master.syntheticMasterTopVariantUrl())
        assertNull("https://cdn.test/a.m3u8".syntheticMasterTopVariantUrl())
    }
}
