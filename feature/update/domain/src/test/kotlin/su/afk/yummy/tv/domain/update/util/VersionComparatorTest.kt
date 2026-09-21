package su.afk.yummy.tv.domain.update.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `equal versions are not newer`() {
        assertFalse(isVersionNewer("1.24.1", "1.24.1"))
        assertFalse(isVersionNewer("1.24.1-beta", "1.24.1-beta"))
    }

    @Test
    fun `missing segments count as zero`() {
        assertFalse(isVersionNewer("1.24", "1.24.0"))
        assertTrue(isVersionNewer("1.24", "1.24.1"))
        assertFalse(isVersionNewer("1.24.1", "1.24"))
    }

    @Test
    fun `numeric segments compare as numbers`() {
        assertTrue(isVersionNewer("1.9.0", "1.10.0"))
        assertFalse(isVersionNewer("1.10.0", "1.9.9"))
    }

    @Test
    fun `stable is newer than beta of the same version`() {
        assertTrue(isVersionNewer("1.24.1-beta", "1.24.1"))
        assertFalse(isVersionNewer("1.24.1", "1.24.1-beta"))
    }

    @Test
    fun `beta of next version is newer than previous stable`() {
        assertTrue(isVersionNewer("1.24.1", "1.24.2-beta"))
        assertFalse(isVersionNewer("1.24.2-beta", "1.24.1"))
    }

    @Test
    fun `fourth segment beta follows previous stable`() {
        assertTrue(isVersionNewer("1.21.1", "1.21.1.1-beta"))
        assertFalse(isVersionNewer("1.21.1.1-beta", "1.21.1"))
    }

    @Test
    fun `fourth segment compares as number`() {
        assertTrue(isVersionNewer("1.21.1.2-beta", "1.21.1.10-beta"))
        assertFalse(isVersionNewer("1.21.1.10-beta", "1.21.1.9-beta"))
        assertFalse(isVersionNewer("1.21.1.2-beta", "1.21.1.2-beta"))
    }

    @Test
    fun `next stable is newer than any fourth segment beta`() {
        assertTrue(isVersionNewer("1.21.1.99-beta", "1.21.2"))
        assertFalse(isVersionNewer("1.21.2", "1.21.1.5-beta"))
    }

    @Test
    fun `garbage segments do not crash`() {
        assertFalse(isVersionNewer("", ""))
        assertTrue(isVersionNewer("abc", "0.0.1"))
    }
}
