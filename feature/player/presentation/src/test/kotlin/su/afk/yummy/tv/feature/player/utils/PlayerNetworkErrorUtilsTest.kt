package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException

class PlayerNetworkErrorUtilsTest {
    @Test
    fun `network throwable is a network failure`() {
        assertTrue(isNetworkResolveFailure(ConnectException("refused"), null))
    }

    @Test
    fun `stringified connect exception is a network failure`() {
        assertTrue(
            isNetworkResolveFailure(
                null,
                "ConnectException: Failed to connect to plapi.cdnvideohub.com/90.156.211.94:443",
            ),
        )
    }

    @Test
    fun `parsing failure is not a network failure`() {
        assertFalse(isNetworkResolveFailure(null, "missing anime_id"))
        assertFalse(isNetworkResolveFailure(IllegalStateException("boom"), null))
    }
}
