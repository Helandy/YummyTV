package su.afk.yummy.tv.data.videodownload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafFileNamesTest {

    @Test
    fun `кириллическое имя укладывается в лимит байтов, а не символов`() {
        val name = CRASH_REPORT_TITLE.toSafeSafName(MAX_DIRECTORY_NAME_BYTES)

        assertTrue(name.utf8Size() <= MAX_DIRECTORY_NAME_BYTES)
        assertTrue(name.length < MAX_DIRECTORY_NAME_BYTES)
    }

    @Test
    fun `surrogate-пара не разрывается пополам`() {
        // "a" + эмодзи (4 байта): в лимит 4 байта влезает только "a"
        val name = "a🍜b".truncateToUtf8Bytes(4)

        assertEquals("a", name)
    }

    @Test
    fun `имя не заканчивается на точку, пробел или подчёркивание`() {
        val name = "Тайтл. ".toSafeSafName(maxBytes = 12)

        assertEquals("Тайтл", name)
    }

    @Test
    fun `запрещённые и control-символы вычищены`() {
        val name = "Тайтл: часть 1/2\u0007".toSafeSafName(MAX_DIRECTORY_NAME_BYTES)

        assertEquals("Тайтл часть 1 2", name)
    }

    @Test
    fun `расширение сохраняется при обрезке длинного имени`() {
        val maxBaseBytes = MAX_FILE_NAME_BYTES - ".mp4".utf8Size()
        val base = CRASH_REPORT_TITLE.toSafeSafName(maxBaseBytes)

        val fileName = "$base.mp4"

        assertTrue(fileName.endsWith(".mp4"))
        assertTrue(fileName.utf8Size() <= MAX_FILE_NAME_BYTES)
    }

    @Test
    fun `ключ имени игнорирует регистр, хвостовую точку и суффикс дубля`() {
        val key = "Тайтл".safNameKey()

        assertEquals(key, "ТАЙТЛ".safNameKey())
        assertEquals(key, "Тайтл.".safNameKey())
        assertEquals(key, "Тайтл (1)".safNameKey())
        assertEquals(key, "Тайтл  (12)".safNameKey())
    }

    @Test
    fun `укороченное провайдером имя опознаётся как та же папка`() {
        val full = CRASH_REPORT_TITLE.toSafeSafName(MAX_DIRECTORY_NAME_BYTES)
        val providerTruncated = full.truncateToUtf8Bytes(120) + " (1)"

        assertTrue(providerTruncated.isTruncatedSafNameOf(full))
    }

    @Test
    fun `короткое чужое имя не считается укороченным вариантом`() {
        assertFalse("Забота".isTruncatedSafNameOf(CRASH_REPORT_TITLE))
    }

    private companion object {
        const val CRASH_REPORT_TITLE = "Забота об одарённой девушке: В престижной школе, полной " +
            "высококлассных учеников, я буду тайно заботиться о самой красивой девушке " +
            "(не имеющей никаких жизненных навыков)"
    }
}
