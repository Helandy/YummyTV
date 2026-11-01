package su.afk.yummy.tv.feature.player.common.service

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.IOException
import java.net.ProtocolException

@UnstableApi
class PlayerLoadErrorHandlingPolicyTest : BaseUnitTest() {

    private val config: PlayerPlaybackConfig = mockk()

    private fun policy(silent: Boolean): PlayerLoadErrorHandlingPolicy {
        every { config.silentReconnectEnabled() } returns silent
        return PlayerLoadErrorHandlingPolicy(config)
    }

    private fun errorInfo(exception: IOException, errorCount: Int = 1) =
        LoadErrorHandlingPolicy.LoadErrorInfo(
            LoadEventInfo(1L, mockk<DataSpec>(relaxed = true), mockk<Uri>(relaxed = true), emptyMap(), 0L, 0L, 0L),
            MediaLoadData(C.DATA_TYPE_MEDIA),
            exception,
            errorCount,
        )

    private fun httpError(code: Int) = HttpDataSource.InvalidResponseCodeException(
        code,
        null,
        null,
        emptyMap(),
        mockk<DataSpec>(relaxed = true),
        ByteArray(0),
    )

    @Test
    fun `тихое переподключение растягивает окно ретраев`() {
        assertEquals(20, policy(silent = true).getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA))
    }

    @Test
    fun `без тихого переподключения - дефолтное число ретраев`() {
        assertEquals(
            DefaultLoadErrorHandlingPolicy().getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA),
            policy(silent = false).getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA),
        )
    }

    @Test
    fun `мёртвые ответы не ретраятся - быстрый фатал для перерезолва`() {
        listOf(400, 401, 403, 404, 410).forEach { code ->
            assertEquals(
                "$code",
                C.TIME_UNSET,
                policy(silent = true).getRetryDelayMsFor(errorInfo(httpError(code))),
            )
        }
    }

    @Test
    fun `таймаут и лимит остаются на дефолтном бэкоффе`() {
        listOf(408, 429, 503).forEach { code ->
            assertNotEquals(
                "$code",
                C.TIME_UNSET,
                policy(silent = true).getRetryDelayMsFor(errorInfo(httpError(code))),
            )
        }
    }

    @Test
    fun `оборванный ответ сдаётся после нескольких попыток`() {
        val truncated = IOException("cut", ProtocolException("unexpected end of stream"))

        assertNotEquals(C.TIME_UNSET, policy(silent = true).getRetryDelayMsFor(errorInfo(truncated, errorCount = 3)))
        assertEquals(C.TIME_UNSET, policy(silent = true).getRetryDelayMsFor(errorInfo(truncated, errorCount = 4)))
    }

    @Test
    fun `обычный сетевой сбой ретраится`() {
        assertNotEquals(
            C.TIME_UNSET,
            policy(silent = true).getRetryDelayMsFor(errorInfo(IOException("no route"), errorCount = 10)),
        )
    }
}
