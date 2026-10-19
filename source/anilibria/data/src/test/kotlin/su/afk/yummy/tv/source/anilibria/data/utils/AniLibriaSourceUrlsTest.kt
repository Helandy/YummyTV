package su.afk.yummy.tv.source.anilibria.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaTarget

class AniLibriaSourceUrlsTest : BaseUnitTest() {

    @Test
    fun `url round trips`() {
        val target = AniLibriaTarget(9542, 12)
        assertEquals("source://anilibria/9542/12", target.toSourceUrl())
        assertEquals(target, parseAniLibriaSourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `foreign or malformed urls are rejected`() {
        assertFalse(isAniLibriaSourceUrl("source://anime4k/1/1/1/x"))
        assertTrue(isAniLibriaSourceUrl("source://anilibria/1/1"))
        assertNull(parseAniLibriaSourceUrl("source://anilibria/1"))
        assertNull(parseAniLibriaSourceUrl("source://anilibria/0/1"))
        assertNull(parseAniLibriaSourceUrl("source://anilibria/a/1"))
    }
}
