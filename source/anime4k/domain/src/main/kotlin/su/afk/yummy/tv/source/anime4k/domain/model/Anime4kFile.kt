package su.afk.yummy.tv.source.anime4k.domain.model

/** Прямая ссылка на mp4 в заданном качестве. */
data class Anime4kFile(
    val quality: Int,
    val bitrate: Int,
    val url: String,
)
