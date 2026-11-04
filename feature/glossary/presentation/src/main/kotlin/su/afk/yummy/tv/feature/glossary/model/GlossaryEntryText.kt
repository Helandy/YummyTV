package su.afk.yummy.tv.feature.glossary.model

/** Термин глоссария с уже подставленными для текущей локали текстами. */
data class GlossaryEntryText(
    val key: String,
    val category: GlossaryCategory,
    val term: String,
    val aliases: String,
    val definition: String,
)
