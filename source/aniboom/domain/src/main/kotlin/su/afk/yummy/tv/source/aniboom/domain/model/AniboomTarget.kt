package su.afk.yummy.tv.source.aniboom.domain.model

/** Адрес серии в источнике: id тайтла в эмбеде, номер серии и озвучка. */
data class AniboomTarget(
    val aniboomId: String,
    val episode: Int,
    val translation: Int,
)
