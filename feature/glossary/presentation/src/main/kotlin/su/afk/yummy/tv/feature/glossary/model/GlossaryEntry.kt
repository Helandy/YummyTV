package su.afk.yummy.tv.feature.glossary.model

import androidx.annotation.StringRes

/** Термин глоссария: тексты лежат в ресурсах и подставляются на экране. */
data class GlossaryEntry(
    val key: String,
    val category: GlossaryCategory,
    @param:StringRes val term: Int,
    @param:StringRes val aliases: Int,
    @param:StringRes val definition: Int,
)
