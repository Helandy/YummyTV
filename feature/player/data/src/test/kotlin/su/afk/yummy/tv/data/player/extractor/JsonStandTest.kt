package su.afk.yummy.tv.data.player.extractor

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/**
 * Страж стенда: если вместо настоящего `org.json` вернётся заглушка android.jar, тесты
 * экстракторов лгали бы (пустой `JSONObject`, а ошибки глотает общий catch).
 */
class JsonStandTest : BaseUnitTest() {

    @Test
    fun `org json parses a real document`() {
        val json = JSONObject("""{"a":{"b":"c"},"n":null}""")

        assertEquals("c", json.getJSONObject("a").getString("b"))
        assertTrue(json.isNull("n"))
    }

    @Test
    fun `org json rejects malformed input`() {
        assertThrows(JSONException::class.java) { JSONObject("not json") }
    }
}
