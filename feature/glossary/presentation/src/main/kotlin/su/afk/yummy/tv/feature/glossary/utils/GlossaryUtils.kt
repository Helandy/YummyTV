package su.afk.yummy.tv.feature.glossary.utils

import android.content.res.Resources
import su.afk.yummy.tv.feature.glossary.model.GlossaryCategory
import su.afk.yummy.tv.feature.glossary.model.GlossaryEntry
import su.afk.yummy.tv.feature.glossary.model.GlossaryEntryText

fun GlossaryEntry.toText(resources: Resources): GlossaryEntryText =
    GlossaryEntryText(
        key = key,
        category = category,
        term = resources.getString(term),
        aliases = resources.getString(aliases),
        definition = resources.getString(definition),
    )

/** Оставляет термины, в названии или вариантах написания которых есть [query]; пустой запрос не фильтрует. */
fun List<GlossaryEntryText>.filterByQuery(query: String): List<GlossaryEntryText> {
    val needle = query.trim()
    if (needle.isEmpty()) return this
    return filter {
        it.term.contains(needle, ignoreCase = true) || it.aliases.contains(needle, ignoreCase = true)
    }
}

/** Группирует термины по разделам в порядке объявления [GlossaryCategory]; пустые разделы пропускает. */
fun List<GlossaryEntryText>.groupByCategory(): List<Pair<GlossaryCategory, List<GlossaryEntryText>>> {
    val grouped = groupBy { it.category }
    return GlossaryCategory.entries.mapNotNull { category -> grouped[category]?.let { category to it } }
}
