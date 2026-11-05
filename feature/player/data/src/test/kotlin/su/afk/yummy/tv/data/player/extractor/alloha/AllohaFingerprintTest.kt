package su.afk.yummy.tv.data.player.extractor.alloha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllohaFingerprintTest {

    private val traits = AllohaDeviceTraits(
        userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 7 Build/AP2A; wv) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Version/4.0 Chrome/131.0.6778.39 Mobile Safari/537.36",
        timeZone = "Europe/Moscow",
        screenLong = 915,
        screenShort = 412,
        language = "ru-RU",
        deviceMemoryGb = 8.0,
        cores = 8,
        canvas = "data:image/png;base64,AAAA",
        webglRenderer = "ARM|Mali-G52",
    )

    @Test
    fun `fingerprint is the page's sha256 over the traits joined with double bars`() {
        // Эталон посчитан независимо (Python): sha256 от строки признаков, UA без номеров версий.
        assertEquals("269ec453df6c428e5afae916f78f4507eb0cf50bef0abacb9b9c8515ba3710c4", allohaFingerprint(traits))
    }

    @Test
    fun `fingerprint is 64 lowercase hex and changes with any trait`() {
        val base = allohaFingerprint(traits)

        assertEquals(64, base.length)
        assertTrue(base.all { it in '0'..'9' || it in 'a'..'f' })
        assertNotEquals(base, allohaFingerprint(traits.copy(cores = 4)))
        assertNotEquals(base, allohaFingerprint(traits.copy(timeZone = "Asia/Yekaterinburg")))
        assertNotEquals(base, allohaFingerprint(traits.copy(canvas = allohaCanvasSample(1))))
    }

    @Test
    fun `dotted version numbers in the user agent do not change the fingerprint`() {
        // Как у страницы: вырезаются только группы цифр с точками, одиночное "14" остаётся.
        val newer = traits.copy(userAgent = traits.userAgent.replace("131.0.6778.39", "140.0.7000.1"))

        assertEquals(allohaFingerprint(traits), allohaFingerprint(newer))
    }

    @Test
    fun `canvas and renderer are stable for a seed and differ between seeds`() {
        assertEquals(allohaCanvasSample(42), allohaCanvasSample(42))
        assertNotEquals(allohaCanvasSample(42), allohaCanvasSample(43))
        assertTrue(allohaCanvasSample(42).startsWith("data:image/png;base64,iVBORw0KGgo"))
        assertEquals(allohaWebglRenderer(7), allohaWebglRenderer(7))
        assertTrue(allohaWebglRenderer(Long.MIN_VALUE).contains("|ANGLE ("))
    }

    @Test
    fun `numbers print the way javascript prints them`() {
        assertEquals("8", jsNumber(8.0))
        assertEquals("0.5", jsNumber(0.5))
        assertEquals("2.625", jsNumber(2.625))
    }
}
