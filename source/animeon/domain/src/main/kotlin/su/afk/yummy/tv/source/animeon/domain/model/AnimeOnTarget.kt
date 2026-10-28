package su.afk.yummy.tv.source.animeon.domain.model

/** Адрес серии в источнике: id тайтла на сайте, озвучка и плеер (из ключа варианта) и номер серии. */
data class AnimeOnTarget(
    val titleKey: String,
    val translationId: Int,
    val playerId: Int,
    val episode: Int,
)
