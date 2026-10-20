package su.afk.yummy.tv.source.anistar.data.utils

/** Серия из JS-массива `playlst` плеер-страницы AniStar: номер из названия и ссылки на HLS по высоте кадра. */
internal data class AnistarPlaylistEntry(
    val number: Int,
    val hlsByQuality: Map<Int, String>,
)
