package su.afk.yummy.tv.core.preferences.auth

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/**
 * На кастомных прошивках AndroidKeyStore бывает нерабочим. Хранилище обязано отличать «шифр
 * недоступен» от «запись битая»: в первом случае сессию терять нельзя, иначе пользователь
 * бесконечно входит заново.
 */
class AuthTokenStorageTest : BaseUnitTest() {

    @Test
    fun `token survives a cipher that cannot decrypt or self-test`() = runTest {
        val store = StoredRecord()
        val healthy = storage(store)
        healthy.write("token")
        val record = store.record

        val broken = storage(store, keystore = cipher(TokenStorageMode.KEYSTORE, broken = true))

        assertEquals("", broken.read())
        assertEquals(record, store.record)
        assertEquals(TokenStorageMode.FALLBACK, broken.mode)
    }

    @Test
    fun `unreadable record is dropped when the cipher is healthy`() = runTest {
        val store = StoredRecord(record = "*not-a-valid-payload")
        val storage = storage(store)

        assertEquals("", storage.read())
        assertNull(store.record)
        assertEquals(TokenStorageMode.KEYSTORE, storage.mode)
    }

    @Test
    fun `transient failure is retried instead of wiping the session`() = runTest {
        val store = StoredRecord()
        val failures = TransientFailures()
        val storage = storage(store, keystore = cipher(TokenStorageMode.KEYSTORE, failures = failures))
        storage.write("token")

        failures.left = 1

        assertEquals("token", storage.read())
        assertNotNull(store.record)
    }

    @Test
    fun `write falls back when keystore encryption fails`() = runTest {
        val store = StoredRecord()
        val failures = mutableListOf<String>()
        val storage = storage(
            state = store,
            keystore = cipher(TokenStorageMode.KEYSTORE, broken = true),
            onFailure = { message, _ -> failures += message },
        )

        storage.write("token")

        assertEquals(TokenStorageMode.FALLBACK, storage.mode)
        assertEquals(TokenStorageMode.FALLBACK, store.mode)
        assertEquals("token", storage.read())
        assertTrue(failures.any { it.contains("encryption failed") })
    }

    @Test
    fun `fallback record is readable after restart`() = runTest {
        val store = StoredRecord()
        storage(store, keystore = cipher(TokenStorageMode.KEYSTORE, broken = true))
            .write("token")

        // Новый экземпляр = перезапуск процесса: режим поднимается из префов.
        val restarted = storage(store, keystore = cipher(TokenStorageMode.KEYSTORE, broken = true))

        assertEquals("token", restarted.read())
    }

    @Test
    fun `blank token clears the record`() = runTest {
        val store = StoredRecord()
        val storage = storage(store)
        storage.write("token")

        storage.write("   ")

        assertNull(store.record)
    }

    private fun storage(
        state: StoredRecord,
        keystore: TokenCipher = cipher(TokenStorageMode.KEYSTORE),
        onFailure: (String, Throwable?) -> Unit = { _, _ -> },
    ) = AuthTokenStorage(
        store = recordStore(state),
        cipherFactory = { mode ->
            if (mode == TokenStorageMode.KEYSTORE) keystore else cipher(TokenStorageMode.FALLBACK)
        },
        onStorageFailure = onFailure,
        sleep = {},
    )

    /** То, что хранилище записало в префы: переживает пересоздание [AuthTokenStorage]. */
    private class StoredRecord(
        var record: String? = null,
        var mode: TokenStorageMode? = null,
    )

    /** Сколько ближайших расшифровок упадут временным отказом. */
    private class TransientFailures(var left: Int = 0)

    private fun recordStore(state: StoredRecord) = mockk<TokenRecordStore> {
        every { readRecord() } answers { state.record }
        every { writeRecord(any()) } answers { state.record = firstArg() }
        every { removeRecord() } answers { state.record = null }
        every { readMode() } answers { state.mode }
        every { writeMode(any()) } answers { state.mode = firstArg() }
    }

    /** [broken] — шифр недоступен целиком, [failures] — временный отказ на N попыток. */
    private fun cipher(
        mode: TokenStorageMode,
        broken: Boolean = false,
        failures: TransientFailures = TransientFailures(),
    ) = mockk<TokenCipher> {
        every { this@mockk.mode } returns mode
        every { encrypt(any()) } answers {
            check(!broken) { "cipher is unavailable" }
            CIPHER_PREFIX + firstArg<String>()
        }
        every { decrypt(any()) } answers {
            check(!broken) { "cipher is unavailable" }
            if (failures.left > 0) {
                failures.left--
                error("transient failure")
            }
            val value = firstArg<String>()
            require(value.startsWith(CIPHER_PREFIX)) { "unreadable record" }
            value.removePrefix(CIPHER_PREFIX)
        }
        every { selfTest() } answers { !broken && failures.left == 0 }
    }

    private companion object {
        const val CIPHER_PREFIX = "enc:"
    }
}
