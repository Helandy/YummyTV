package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.feature.details.model.TitleScript

private fun String.titleScript(): TitleScript {
    var latin = false
    var other = false
    for (char in this) {
        when (Character.UnicodeScript.of(char.code)) {
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HAN -> return TitleScript.Japanese
            Character.UnicodeScript.LATIN -> latin = true
            Character.UnicodeScript.CYRILLIC -> other = true
            else -> Unit
        }
    }
    return if (latin && !other) TitleScript.English else TitleScript.Alternative
}

fun List<String>.groupTitlesByScript(): List<Pair<TitleScript, List<String>>> {
    val grouped = map { it.trim() }.filter { it.isNotEmpty() }.distinct().groupBy { it.titleScript() }
    return TitleScript.entries.mapNotNull { script -> grouped[script]?.let { script to it } }
}
