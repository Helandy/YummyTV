package su.afk.yummy.tv.data.player.extractor.alloha

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class AllohaMirrorPickerTest : BaseUnitTest() {

    private val probed = mutableListOf<String>()

    private fun picker(vararg dead: String) = AllohaMirrorPicker { url ->
        probed += url
        dead.none { url.startsWith(it) }
    }

    @Test
    fun `a single mirror is used without a probe`() {
        val picked = picker().pick(listOf("https://a/1.m3u8"))

        assertEquals("https://a/1.m3u8", picked)
        assertEquals(emptyList<String>(), probed)
    }

    @Test
    fun `a working first mirror stays first`() {
        val picked = picker().pick(listOf("https://a/1.m3u8", "https://b/1.m3u8"))

        assertEquals("https://a/1.m3u8", picked)
    }

    @Test
    fun `a dead first mirror gives way to the last one`() {
        val picked = picker("https://a/").pick(listOf("https://a/1.m3u8", "https://b/1.m3u8"))

        assertEquals("https://b/1.m3u8", picked)
    }

    @Test
    fun `the verdict is cached per host, so qualities on one node cost one probe`() {
        val picker = picker("https://a/")

        picker.pick(listOf("https://a/1080.m3u8", "https://b/1080.m3u8"))
        picker.pick(listOf("https://a/720.m3u8", "https://b/720.m3u8"))
        val last = picker.pick(listOf("https://a/480.m3u8", "https://b/480.m3u8"))

        assertEquals("https://b/480.m3u8", last)
        assertEquals(listOf("https://a/1080.m3u8"), probed)
    }

    @Test
    fun `different hosts are probed separately`() {
        val picker = picker("https://a/")

        picker.pick(listOf("https://a/1.m3u8", "https://b/1.m3u8"))
        val second = picker.pick(listOf("https://c/1.m3u8", "https://d/1.m3u8"))

        assertEquals("https://c/1.m3u8", second)
        assertEquals(listOf("https://a/1.m3u8", "https://c/1.m3u8"), probed)
    }
}
