package su.afk.yummy.tv.core.utils.kodik

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class ContinueWatchingImageResolverTest : BaseUnitTest() {

    private fun models(screenshotUrl: String = "", episodeUrl: String = "", posterUrls: List<String> = listOf(POSTER)) =
        resolveContinueWatchingImageModels(
            screenshotUrl = screenshotUrl,
            episodeUrl = episodeUrl,
            posterUrls = posterUrls,
            kodikThumbnailModel = { KodikThumbnail(it, ANIME_ID, "1") },
        )

    @Test
    fun `kodikres screenshot is loaded directly instead of being parsed as iframe`() {
        assertEquals(listOf<Any>(KODIKRES_SCREENSHOT, POSTER), models(screenshotUrl = KODIKRES_SCREENSHOT))
    }

    @Test
    fun `kodik iframe screenshot resolves to episode thumbnail`() {
        assertEquals(listOf<Any>(KodikThumbnail(KODIK_IFRAME, ANIME_ID, "1"), POSTER), models(screenshotUrl = KODIK_IFRAME))
    }

    @Test
    fun `kodik episode url is used when screenshot is not an iframe`() {
        assertEquals(
            listOf<Any>(KodikThumbnail(KODIK_IFRAME, ANIME_ID, "1"), POSTER),
            models(screenshotUrl = KODIKRES_SCREENSHOT, episodeUrl = KODIK_IFRAME),
        )
    }

    @Test
    fun `falls back to poster without screenshot and kodik source`() {
        assertEquals(listOf<Any>(POSTER), models(episodeUrl = "https://alloha.yani.tv/?token_movie=x"))
    }

    @Test
    fun `all poster versions follow the episode thumbnail as fallbacks`() {
        assertEquals(
            listOf<Any>(KODIKRES_SCREENSHOT, "big", "small"),
            models(screenshotUrl = KODIKRES_SCREENSHOT, posterUrls = listOf("big", "small")),
        )
    }

    @Test
    fun `image urls are not kodik iframes`() {
        assertEquals(false, KODIKRES_SCREENSHOT.isKodikIframeUrl())
        assertEquals(true, KODIK_IFRAME.isKodikIframeUrl())
    }

    private companion object {
        const val ANIME_ID = 2230
        const val POSTER = "https://static.yani.tv/posters/full/1.jpg"
        const val KODIKRES_SCREENSHOT = "https://i.kodikres.com/screenshots/seria/1060462/1.jpg"
        const val KODIK_IFRAME =
            "https://kodikplayer.com/season/29435/ee4d9ec8d847c259c02dc568ba40b0f5/720p?episode=1"
    }
}
