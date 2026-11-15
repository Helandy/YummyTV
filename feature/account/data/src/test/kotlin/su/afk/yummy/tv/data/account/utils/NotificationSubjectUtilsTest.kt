package su.afk.yummy.tv.data.account.utils

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class NotificationSubjectUtilsTest : BaseUnitTest() {

    private fun subject(json: String) = Json.parseToJsonElement(json).toNotificationSubject()

    @Test
    fun `anime title and id are taken from data anime`() {
        val data = """{"comment":{"id":1},"anime":{"title":"Тайтл","anime_id":28888,"anime_url":"x"}}"""

        assertEquals(NotificationSubject("Тайтл", 28888), subject(data))
    }

    @Test
    fun `post title has no anime id`() {
        val data = """{"comment":{"id":1},"post":{"id":2516,"title":"Пост"}}"""

        assertEquals(NotificationSubject("Пост", null), subject(data))
    }

    @Test
    fun `unexpected shapes give null`() {
        assertNull(subject("[]"))
        assertNull(subject("""{"comment":{"id":1}}"""))
        assertNull(subject("""{"anime":{"title":" "}}"""))
        assertNull(subject("""{"anime":"oops"}"""))
    }
}
