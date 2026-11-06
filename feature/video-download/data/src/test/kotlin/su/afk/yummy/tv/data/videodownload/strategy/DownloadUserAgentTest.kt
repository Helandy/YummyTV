package su.afk.yummy.tv.data.videodownload.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/** User-Agent загрузок: свой UA экстрактора главнее, иначе UA выбранного профиля, а не вшитый в код. */
class DownloadUserAgentTest : BaseUnitTest() {

    private val iframe = "https://player.example/embed/1"

    @Test
    fun `default strategy fills the user agent of the selected profile`() {
        val headers = DefaultDownloadStrategy.decorateHeaders(emptyMap(), iframe, defaultUserAgent = "Profile/Linux")

        assertEquals("Profile/Linux", headers["User-Agent"])
    }

    @Test
    fun `extractor user agent wins over the profile`() {
        val headers = DefaultDownloadStrategy.decorateHeaders(
            mapOf("user-agent" to "Extractor/UA"),
            iframe,
            defaultUserAgent = "Profile/Linux",
        )

        assertEquals("Extractor/UA", headers["User-Agent"])
        assertNull(headers["user-agent"])
    }

    @Test
    fun `blank extractor user agent is replaced by the profile`() {
        val headers = DefaultDownloadStrategy.decorateHeaders(
            mapOf("User-Agent" to " "),
            iframe,
            defaultUserAgent = "Profile/Android",
        )

        assertEquals("Profile/Android", headers["User-Agent"])
    }

    @Test
    fun `cvh and alloha strategies also fill the profile user agent`() {
        assertEquals("P", CvhDownloadStrategy().decorateHeaders(emptyMap(), iframe, "P")["User-Agent"])
        assertEquals("P", AllohaDownloadStrategy().decorateHeaders(emptyMap(), iframe, "P")["User-Agent"])
    }

    @Test
    fun `cvh keeps no referer or origin and alloha pins its origin`() {
        val cvh = CvhDownloadStrategy().decorateHeaders(emptyMap(), iframe, "P")
        val alloha = AllohaDownloadStrategy().decorateHeaders(emptyMap(), iframe, "P")

        assertNull(cvh["Referer"])
        assertNull(cvh["Origin"])
        assertEquals("https://alloha.yani.tv", alloha["Origin"])
    }
}
