package su.afk.yummy.tv.core.preferences.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * На кастомных прошивках AndroidKeyStore бывает нерабочим. Хранилище обязано отличать «шифр
 * недоступен» от «запись битая»: в первом случае сессию терять нельзя, иначе пользователь
 * бесконечно входит заново.
 */
class AuthTokenStorageTest {

    @Test
    fun `token survives a cipher that cannot decrypt or self-test`() = runTest {
        val store = FakeRecordStore()
        val healthy = storage(store)
        healthy.write("token")
        val record = store.record

        val broken = storage(store, keystore = FakeCipher(TokenStorageMode.KEYSTORE, broken = true))

        assertEquals("", broken.read())
        assertEquals(record, store.record)
        assertEquals(TokenStorageMode.FALLBACK, broken.mode)
    }

    @Test
    fun `unreadable record is dropped when the cipher is healthy`() = runTest {
        val store = FakeRecordStore(record = "*not-a-valid-payload")
        val storage = storage(store)

        assertEquals("", storage.read())
        assertNull(store.record)
        assertEquals(TokenStorageMode.KEYSTORE, storage.mode)
    }

    @Test
    fun `transient failure is retried instead of wiping the session`() = runTest {
        val store = FakeRecordStore()
        val cipher = FakeCipher(TokenStorageMode.KEYSTORE)
        val storage = storage(store, keystore = cipher)
        storage.write("token")

        cipher.failuresLeft = 1

        assertEquals("token", storage.read())
        assertNotNull(store.record)
    }

    @Test
    fun `write falls back when keystore encryption fails`() = runTest {
        val store = FakeRecordStore()
        val failures = mutableListOf<String>()
        val storage = storage(
            store = store,
            keystore = FakeCipher(TokenStorageMode.KEYSTORE, broken = true),
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
        val store = FakeRecordStore()
        storage(store, keystore = FakeCipher(TokenStorageMode.KEYSTORE, broken = true))
            .write("token")

        // Новый экземпляр = перезапуск процесса: режим поднимается из префов.
        val restarted = storage(store, keystore = FakeCipher(TokenStorageMode.KEYSTORE, broken = true))

        assertEquals("token", restarted.read())
    }

    @Test
    fun `blank token clears the record`() = runTest {
        val store = FakeRecordStore()
        val storage = storage(store)
        storage.write("token")

        storage.write("   ")

        assertNull(store.record)
    }

    private fun storage(
        store: FakeRecordStore,
        keystore: FakeCipher = FakeCipher(TokenStorageMode.KEYSTORE),
        onFailure: (String, Throwable?) -> Unit = { _, _ -> },
    ) = AuthTokenStorage(
        store = store,
        cipherFactory = { mode ->
            if (mode == TokenStorageMode.KEYSTORE) keystore else FakeCipher(TokenStorageMode.FALLBACK)
        },
        onStorageFailure = onFailure,
        sleep = {},
    )

    private class FakeRecordStore(
        var record: String? = null,
        var mode: TokenStorageMode? = null,
    ) : TokenRecordStore {
        override fun readRecord(): String? = record

        override fun writeRecord(record: String) {
            this.record = record
        }

        override fun removeRecord() {
            record = null
        }

        override fun readMode(): TokenStorageMode? = mode

        override fun writeMode(mode: TokenStorageMode) {
            this.mode = mode
        }
    }

    /** [broken] — шифр недоступен целиком, [failuresLeft] — временный отказ на N попыток. */
    private class FakeCipher(
        override val mode: TokenStorageMode,
        private val broken: Boolean = false,
        var failuresLeft: Int = 0,
    ) : TokenCipher {
        override fun encrypt(value: String): String {
            check(!broken) { "cipher is unavailable" }
            return PREFIX + value
        }

        override fun decrypt(value: String): String {
            check(!broken) { "cipher is unavailable" }
            if (failuresLeft > 0) {
                failuresLeft--
                error("transient failure")
            }
            require(value.startsWith(PREFIX)) { "unreadable record" }
            return value.removePrefix(PREFIX)
        }

        override fun selfTest(): Boolean = !broken && failuresLeft == 0

        private companion object {
            const val PREFIX = "enc:"
        }
    }
}
