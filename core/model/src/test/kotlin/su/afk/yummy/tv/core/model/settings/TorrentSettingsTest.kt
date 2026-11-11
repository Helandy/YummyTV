package su.afk.yummy.tv.core.model.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class TorrentSettingsTest : BaseUnitTest() {

    private val configured = TorrentSettings(
        enabled = true,
        torrServerUrl = "http://192.168.1.2:8090",
        jacRedUrl = "https://my-jacred.example",
        jackettUrl = "http://192.168.1.10:9117",
    )

    @Test
    fun `indexers work alone or together`() {
        assertEquals(listOf(TorrentIndexer.JACRED), configured.activeIndexers)
        assertEquals(
            listOf(TorrentIndexer.JACKETT),
            configured.copy(jacRedEnabled = false, jackettEnabled = true).activeIndexers,
        )
        assertEquals(
            listOf(TorrentIndexer.JACRED, TorrentIndexer.JACKETT),
            configured.copy(jackettEnabled = true).activeIndexers,
        )
    }

    @Test
    fun `blank jacred url falls back to default`() {
        assertEquals(TorrentSettings.DEFAULT_INDEXER_URL, configured.copy(jacRedUrl = "").effectiveJacRedUrl)
    }

    @Test
    fun `jackett without address is skipped and not configured alone`() {
        assertTrue(configured.isConfigured)
        val onlyJackett = configured.copy(jacRedEnabled = false, jackettEnabled = true, jackettUrl = "")
        assertFalse(onlyJackett.isConfigured)
        assertFalse(configured.copy(jacRedEnabled = false).isConfigured)
    }
}
