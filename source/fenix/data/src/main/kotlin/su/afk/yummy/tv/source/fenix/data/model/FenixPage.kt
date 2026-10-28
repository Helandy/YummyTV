package su.afk.yummy.tv.source.fenix.data.model

/**
 * Разобранный адрес страницы плеера Fenix: [path] — `/vod/{токен}` или `/embed/{токен}`, [isVod] — страница одной серии,
 * [episode] и [edition] — параметры `ep` и `edition` страницы `/embed/`.
 */
internal data class FenixPage(
    val path: String,
    val isVod: Boolean,
    val episode: Int?,
    val edition: String?,
)
