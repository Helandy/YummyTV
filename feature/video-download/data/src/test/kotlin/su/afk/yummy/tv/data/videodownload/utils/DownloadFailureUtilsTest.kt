package su.afk.yummy.tv.data.videodownload.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.videodownload.worker.utils.isTransientDownloadFailure
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DownloadFailureUtilsTest : BaseUnitTest() {

    @Test
    fun `таймаут сокета считается временным сбоем`() {
        assertTrue(SocketTimeoutException("timeout").isTransientDownloadFailure())
    }

    @Test
    fun `потеря DNS в цепочке причин считается временным сбоем`() {
        val failure = IOException("download failed", UnknownHostException("cdn.example"))

        assertTrue(failure.isTransientDownloadFailure())
    }

    @Test
    fun `сброс соединения считается временным сбоем`() {
        val failure = RuntimeException("wrapped", SocketException("Connection reset"))

        assertTrue(failure.isTransientDownloadFailure())
    }

    @Test
    fun `неизвестная ошибка не считается временной`() {
        assertFalse(IllegalStateException("broken").isTransientDownloadFailure())
    }
}
