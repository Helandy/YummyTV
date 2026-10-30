package su.afk.yummy.tv.core.network.dns

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class BlockedHostDnsTest {

    private val real = listOf(InetAddress.getByName("138.199.36.7"))
    private val loopback = listOf(InetAddress.getByName("127.0.0.1"))
    private var now = 0L
    private var fallbackCalls = 0

    private fun fallback() = Dns {
        fallbackCalls++
        real
    }

    private fun dns(system: Dns, fallback: Dns = fallback()) =
        BlockedHostDns(setOf(HOST), fallback, system) { now }

    @Test
    fun `normal system answer is used without the fallback`() {
        val answer = listOf(InetAddress.getByName("1.2.3.4"))

        assertEquals(answer, dns(system = { answer }).lookup(HOST))
        assertEquals(0, fallbackCalls)
    }

    @Test
    fun `loopback answer means blocking and goes to the fallback`() {
        assertEquals(real, dns(system = { loopback }).lookup(HOST))
        assertEquals(1, fallbackCalls)
    }

    @Test
    fun `resolve error goes to the fallback`() {
        assertEquals(real, dns(system = { throw UnknownHostException("no dns") }).lookup(HOST))
    }

    @Test
    fun `fallback answer is cached for a while`() {
        val dns = dns(system = { loopback })

        dns.lookup(HOST)
        dns.lookup(HOST)
        assertEquals(1, fallbackCalls)

        now += 6 * 60 * 1000L
        dns.lookup(HOST)
        assertEquals(2, fallbackCalls)
    }

    @Test
    fun `other hosts never touch the fallback`() {
        val answer = listOf(InetAddress.getByName("5.6.7.8"))

        assertEquals(answer, dns(system = { answer }).lookup("kodik.example"))
        assertEquals(loopback, dns(system = { loopback }).lookup("localhost.example"))
        assertEquals(0, fallbackCalls)
    }

    @Test
    fun `failed fallback is not hidden behind the blocked answer`() {
        val dns = dns(system = { loopback }, fallback = { throw UnknownHostException("doh down") })

        assertThrows(UnknownHostException::class.java) { dns.lookup(HOST) }
    }

    private companion object {
        const val HOST = "image.tmdb.org"
    }
}
