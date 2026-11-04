package su.afk.yummy.tv.feature.glossary

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.feature.glossary.model.GlossaryCategory
import su.afk.yummy.tv.feature.glossary.model.GlossaryEntryText
import su.afk.yummy.tv.feature.glossary.utils.filterByQuery
import su.afk.yummy.tv.feature.glossary.utils.groupByCategory

class GlossaryUtilsTest {

    private fun entry(key: String, category: GlossaryCategory, term: String, aliases: String) =
        GlossaryEntryText(key, category, term, aliases, "definition")

    private val entries = listOf(
        entry("dub", GlossaryCategory.VOICE, "Дубляж", "dub, дубляж"),
        entry("title", GlossaryCategory.SLANG, "Тайтл", "title, тайтл"),
        entry("hardsub", GlossaryCategory.VOICE, "Хардсаб", "hardsub, хардсаб"),
    )

    @Test
    fun `blank query keeps every entry`() {
        assertEquals(entries, entries.filterByQuery("  "))
    }

    @Test
    fun `query matches term ignoring case`() {
        assertEquals(listOf("title"), entries.filterByQuery("ТАЙТЛ").map { it.key })
    }

    @Test
    fun `query matches alias`() {
        assertEquals(listOf("hardsub"), entries.filterByQuery("hardSUB").map { it.key })
    }

    @Test
    fun `groups follow category declaration order and skip empty ones`() {
        val grouped = entries.groupByCategory()

        assertEquals(listOf(GlossaryCategory.SLANG, GlossaryCategory.VOICE), grouped.map { it.first })
        assertEquals(listOf("dub", "hardsub"), grouped.last().second.map { it.key })
    }
}
