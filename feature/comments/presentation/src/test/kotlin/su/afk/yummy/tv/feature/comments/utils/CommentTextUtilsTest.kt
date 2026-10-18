package su.afk.yummy.tv.feature.comments.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class CommentTextUtilsTest : BaseUnitTest() {

    @Test
    fun `nick tag becomes mention and other bb codes are removed`() {
        assertEquals(
            "@vadcapi за первые 15 серий втянешься)",
            "[ник]vadcapi[/ник] за [b]первые[/b] 15 серий втянешься)".stripBbCode(),
        )
    }
}
