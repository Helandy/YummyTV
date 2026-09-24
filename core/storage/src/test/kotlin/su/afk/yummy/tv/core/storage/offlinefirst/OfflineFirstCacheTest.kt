package su.afk.yummy.tv.core.storage.offlinefirst

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private data class FakeCache(val value: String)

class OfflineFirstCacheTest {

    @Test
    fun `fresh cache is served without hitting the network`() = runTest {
        var fetchCalled = false

        val result = offlineFirstCache(
            read = { FakeCache("cached") },
            isFresh = { true },
            toDomain = { it.value },
            fetchAndSave = {
                fetchCalled = true
                FakeCache("network")
            },
        )

        assertEquals("cached", result)
        assertFalse(fetchCalled)
    }

    @Test
    fun `stale or missing cache falls through to the network`() = runTest {
        val result = offlineFirstCache(
            read = { FakeCache("stale") },
            isFresh = { false },
            toDomain = { it.value },
            fetchAndSave = { FakeCache("network") },
        )

        assertEquals("network", result)
    }

    @Test
    fun `network failure falls back to the stale cache`() = runTest {
        val result = offlineFirstCache(
            read = { FakeCache("stale") },
            isFresh = { false },
            toDomain = { it.value },
            fetchAndSave = { throw IllegalStateException("network down") },
        )

        assertEquals("stale", result)
    }

    @Test
    fun `network failure with no cache throws by default`() = runTest {
        var thrown: Throwable? = null
        try {
            offlineFirstCache<FakeCache, String>(
                read = { null },
                isFresh = { false },
                toDomain = { it.value },
                fetchAndSave = { throw IllegalStateException("network down") },
            )
        } catch (error: Throwable) {
            thrown = error
        }

        assertTrue(thrown is IllegalStateException)
    }

    @Test
    fun `network failure with no cache uses custom onMissing fallback`() = runTest {
        val result = offlineFirstCache<FakeCache, String>(
            read = { null },
            isFresh = { false },
            toDomain = { it.value },
            fetchAndSave = { throw IllegalStateException("network down") },
            onMissing = { "fallback" },
        )

        assertEquals("fallback", result)
    }

    @Test
    fun `a real cache hit that maps to a null domain value is not mistaken for a missing cache`() =
        runTest {
            var onMissingCalled = false

            val result = offlineFirstCache(
                read = { FakeCache("stale") },
                isFresh = { false },
                toDomain = { null as String? },
                fetchAndSave = { throw IllegalStateException("network down") },
                onMissing = {
                    onMissingCalled = true
                    "fallback"
                },
            )

            assertEquals(null, result)
            assertFalse(onMissingCalled)
        }

    @Test
    fun `forceRefresh skips the upfront cache read but still falls back on error`() = runTest {
        var readCalls = 0

        val result = offlineFirstCache(
            forceRefresh = true,
            read = {
                readCalls++
                FakeCache("stale")
            },
            isFresh = { true },
            toDomain = { it.value },
            fetchAndSave = { throw IllegalStateException("network down") },
        )

        assertEquals("stale", result)
        assertEquals(1, readCalls)
    }

    @Test
    fun `cancellation is propagated instead of triggering the fallback`() = runTest {
        var onMissingCalled = false

        val job = launch {
            offlineFirstCache<FakeCache, String>(
                read = { null },
                isFresh = { false },
                toDomain = { it.value },
                fetchAndSave = { awaitCancellation() },
                onMissing = {
                    onMissingCalled = true
                    "fallback"
                },
            )
        }
        runCurrent()
        job.cancelAndJoin()

        assertFalse(onMissingCalled)
    }

    @Test
    fun `transform is applied uniformly across fresh cache, network and fallback branches`() =
        runTest {
            val calls = mutableListOf<String>()
            val transform: suspend (String) -> String = {
                calls.add(it)
                "$it+transformed"
            }

            val freshResult = offlineFirstCache(
                read = { FakeCache("cached") },
                isFresh = { true },
                toDomain = { it.value },
                fetchAndSave = { FakeCache("network") },
                transform = transform,
            )
            val networkResult = offlineFirstCache<FakeCache, String>(
                read = { null },
                isFresh = { false },
                toDomain = { it.value },
                fetchAndSave = { FakeCache("network") },
                transform = transform,
            )
            val fallbackResult = offlineFirstCache(
                read = { FakeCache("stale") },
                isFresh = { false },
                toDomain = { it.value },
                fetchAndSave = { throw IllegalStateException("network down") },
                transform = transform,
            )

            assertEquals("cached+transformed", freshResult)
            assertEquals("network+transformed", networkResult)
            assertEquals("stale+transformed", fallbackResult)
            assertEquals(listOf("cached", "network", "stale"), calls)
        }
}
