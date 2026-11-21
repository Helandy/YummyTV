package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class InFlightRequestUtilsTest {

    @Test
    fun `already completed request is returned and removed without recursive update`() {
        val requests = ConcurrentHashMap<String, Deferred<Int>>()
        val completed = CompletableDeferred(42)

        val result = requests.getOrCreateInFlight("anime") { completed }

        assertSame(completed, result)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `already failed and cancelled requests are removed`() {
        val requests = ConcurrentHashMap<String, Deferred<Int>>()
        val failed = CompletableDeferred<Int>().apply { completeExceptionally(IllegalStateException("test")) }
        val cancelled = CompletableDeferred<Int>().apply { cancel() }

        assertSame(failed, requests.getOrCreateInFlight("failed") { failed })
        assertSame(cancelled, requests.getOrCreateInFlight("cancelled") { cancelled })
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `active request is shared and a new request starts after completion`() {
        val requests = ConcurrentHashMap<String, Deferred<Int>>()
        val first = CompletableDeferred<Int>()
        val second = CompletableDeferred<Int>()
        val creates = AtomicInteger()

        val original = requests.getOrCreateInFlight("anime") {
            creates.incrementAndGet()
            first
        }
        val shared = requests.getOrCreateInFlight("anime") {
            creates.incrementAndGet()
            second
        }

        assertSame(first, original)
        assertSame(first, shared)
        assertEquals(1, creates.get())
        assertSame(first, requests["anime"])

        first.complete(42)
        assertFalse(requests.containsKey("anime"))
        val next = requests.getOrCreateInFlight("anime") {
            creates.incrementAndGet()
            second
        }
        assertSame(second, next)
        assertEquals(2, creates.get())
    }

    @Test
    fun `simultaneous callers share a single active request`() {
        val requests = ConcurrentHashMap<String, Deferred<Int>>()
        val shared = CompletableDeferred<Int>()
        val creates = AtomicInteger()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val callers = List(8) {
                executor.submit<Deferred<Int>> {
                    check(start.await(5, TimeUnit.SECONDS))
                    requests.getOrCreateInFlight("anime") {
                        creates.incrementAndGet()
                        shared
                    }
                }
            }
            start.countDown()
            callers.forEach { assertSame(shared, it.get(5, TimeUnit.SECONDS)) }
            assertEquals(1, creates.get())
            shared.complete(42)
            assertTrue(requests.isEmpty())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `completion does not remove a newer request for the same key`() {
        val requests = ConcurrentHashMap<String, Deferred<Int>>()
        val old = CompletableDeferred<Int>()
        val newer = CompletableDeferred<Int>()
        requests.getOrCreateInFlight("anime") { old }
        requests["anime"] = newer

        old.complete(42)

        assertSame(newer, requests["anime"])
    }
}
