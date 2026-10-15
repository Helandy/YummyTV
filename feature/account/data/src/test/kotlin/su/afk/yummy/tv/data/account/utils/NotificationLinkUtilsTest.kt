package su.afk.yummy.tv.data.account.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.NotificationLink

class NotificationLinkUtilsTest : BaseUnitTest() {

    @Test
    fun `catalog links are extracted in order with plain labels`() {
        val html = """Анонсировано аниме <a href="/catalog/item/b-2">Название &amp; 2</a>.<br>
            Вы смотрели: «<a href="/catalog/item/a-1">Название</a>»."""

        assertEquals(
            listOf(NotificationLink("Название & 2", "b-2"), NotificationLink("Название", "a-1")),
            html.toNotificationLinks(),
        )
    }

    @Test
    fun `duplicates and non catalog links are dropped`() {
        val html = """<a href="/catalog/item/x">X</a> <a href="/catalog/item/x">X</a> <a href="/user/1">Ник</a>"""

        assertEquals(listOf(NotificationLink("X", "x")), html.toNotificationLinks())
    }

    @Test
    fun `links survive a json round trip and broken json is empty`() {
        val links = listOf(NotificationLink("A", "a"))

        assertEquals(links, links.toLinksJson().fromLinksJson())
        assertEquals(emptyList<NotificationLink>(), "oops".fromLinksJson())
    }
}
