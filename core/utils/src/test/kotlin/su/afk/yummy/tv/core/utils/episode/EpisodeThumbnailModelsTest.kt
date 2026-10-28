package su.afk.yummy.tv.core.utils.episode

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class EpisodeThumbnailModelsTest : BaseUnitTest() {

    private fun models(
        screenshotUrl: String = "",
        episodeUrl: String = "",
        posterUrls: List<String> = listOf(POSTER),
        animeId: Int = ANIME_ID,
    ) = episodeThumbnailModels(
        thumbnail = episodeThumbnailOf(animeId, "1", screenshotUrl, episodeUrl),
        posterUrls = posterUrls,
    )

    @Test
    fun `kodikres screenshot is kept as a direct image hint instead of being parsed as iframe`() {
        assertEquals(
            listOf<Any>(EpisodeThumbnail(ANIME_ID, "1", imageUrl = KODIKRES_SCREENSHOT), POSTER),
            models(screenshotUrl = KODIKRES_SCREENSHOT),
        )
    }

    @Test
    fun `kodik iframe screenshot becomes the iframe hint`() {
        assertEquals(
            listOf<Any>(EpisodeThumbnail(ANIME_ID, "1", iframeUrl = KODIK_IFRAME), POSTER),
            models(screenshotUrl = KODIK_IFRAME),
        )
    }

    @Test
    fun `kodik episode url is used when screenshot is empty`() {
        assertEquals(
            listOf<Any>(EpisodeThumbnail(ANIME_ID, "1", iframeUrl = KODIK_IFRAME), POSTER),
            models(episodeUrl = KODIK_IFRAME),
        )
    }

    @Test
    fun `entry from another player still asks for the episode thumbnail`() {
        assertEquals(
            listOf<Any>(EpisodeThumbnail(ANIME_ID, "1"), POSTER),
            models(episodeUrl = "https://alloha.yani.tv/?token_movie=x"),
        )
    }

    @Test
    fun `without anime id and hints only the poster remains`() {
        assertEquals(listOf<Any>(POSTER), models(animeId = 0))
    }

    @Test
    fun `all poster versions follow the episode thumbnail as fallbacks`() {
        assertEquals(
            listOf<Any>(EpisodeThumbnail(ANIME_ID, "1", imageUrl = KODIKRES_SCREENSHOT), "big", "small"),
            models(screenshotUrl = KODIKRES_SCREENSHOT, posterUrls = listOf("big", "small")),
        )
    }

    private companion object {
        const val ANIME_ID = 2230
        const val POSTER = "https://static.yani.tv/posters/full/1.jpg"
        const val KODIKRES_SCREENSHOT = "https://i.kodikres.com/screenshots/seria/1060462/1.jpg"
        const val KODIK_IFRAME =
            "https://kodikplayer.com/season/29435/ee4d9ec8d847c259c02dc568ba40b0f5/720p?episode=1"
    }
}
