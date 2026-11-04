package su.afk.yummy.tv.data.player.extractor.alloha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Эталонные значения посчитаны независимой реализацией того же алгоритма (Python-порт эталонного
 * парсера): перестановка должна совпасть с ней символ в символ, иначе `bnsi` отвечает отказом.
 */
class AllohaBorthTest {

    @Test
    fun `short seeds are permuted by the page algorithm`() {
        assertEquals("", allohaBorthTail(""))
        assertEquals("a", allohaBorthTail("a"))
        assertEquals("ba", allohaBorthTail("ab"))
        assertEquals("cbade", allohaBorthTail("abcde"))
        assertEquals("lnhgpiacojefmkbd", allohaBorthTail("abcdefghijklmnop"))
    }

    @Test
    fun `a real sized seed matches the reference permutation`() {
        val seed = "aBCdEFgHIjKLmNOpQRsTUvWXyZAbCDeFGhIJkLMnOPqRStUVwXYzABcDEfGHiJKlMNoPQrSTuVWxYZaBCdEFgHIj"

        assertEquals(
            "JJXHjKkVIlCTgMLwHNNICoMXdPdQEQnYFrsjuSOzVTEWWyPAxZOKYAqBZbFRaCRcBDvLEeSDfFgUGGtaHhpmiIUB",
            allohaBorthTail(seed),
        )
    }

    @Test
    fun `permutation keeps the same characters`() {
        val seed = "Zq9-_xY0abcdEFGH12345"

        assertEquals(seed.toList().sorted(), allohaBorthTail(seed).toList().sorted())
    }

    @Test
    fun `header is a 64 char hex fingerprint then the permuted seed`() {
        val header = allohaBorthHeader("abcde")

        val (fingerprint, tail) = header.split("|")
        assertEquals(64, fingerprint.length)
        assertTrue(fingerprint.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals("cbade", tail)
    }
}
