package su.afk.yummy.tv.core.model.error

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.IOException

class NetworkErrorsTest : BaseUnitTest() {

    private fun responseError(status: HttpStatusCode): ResponseException {
        val response = mockk<HttpResponse> { every { this@mockk.status } returns status }
        return ResponseException(response, "cached")
    }

    @Test
    fun `обрыв связи и мусор вместо JSON оставляют мутацию в очереди`() {
        assertTrue(IOException("no route").isRetryableMutationFailure())
        assertTrue(SerializationException("<html>captive portal</html>").isRetryableMutationFailure())
    }

    @Test
    fun `шлюзовые и временные статусы оставляют мутацию в очереди`() {
        listOf(500, 502, 503, 504, 408, 425, 429).forEach { code ->
            assertTrue("$code", responseError(HttpStatusCode.fromValue(code)).isRetryableMutationFailure())
        }
    }

    @Test
    fun `постоянный отказ и посторонняя ошибка не попадают в очередь`() {
        listOf(400, 401, 403, 404, 422).forEach { code ->
            assertFalse("$code", responseError(HttpStatusCode.fromValue(code)).isRetryableMutationFailure())
        }
        assertFalse(IllegalStateException("bug").isRetryableMutationFailure())
    }
}
