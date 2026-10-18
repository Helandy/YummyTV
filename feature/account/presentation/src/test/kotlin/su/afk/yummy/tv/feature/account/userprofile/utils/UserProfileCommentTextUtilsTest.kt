package su.afk.yummy.tv.feature.account.userprofile.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class UserProfileCommentTextUtilsTest : BaseUnitTest() {

    @Test
    fun `leading nick becomes reply target and leaves text`() {
        val text = "[ник]vadcapi[/ник] за первые 15 серий втянешься)"

        assertEquals("vadcapi", text.commentReplyTo())
        assertEquals("за первые 15 серий втянешься)", text.toCommentPreviewText())
    }

    @Test
    fun `inner nick becomes mention and spoiler is hidden`() {
        val text = "Согласен с [ник]Nord[/ник], [спойлер=\"финал\"]все умрут[/спойлер] [b]жесть[/b]"

        assertNull(text.commentReplyTo())
        assertEquals("Согласен с @Nord, … жесть", text.toCommentPreviewText())
    }
}
