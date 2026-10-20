package su.afk.yummy.tv.source.aniboom.domain.model

/** Озвучка в AniBoom: [translation] — её id в эмбеде (не путать с id озвучки на сайте). */
data class AniboomDubbing(
    val title: String,
    val translation: Int,
)
