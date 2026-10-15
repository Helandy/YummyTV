package su.afk.yummy.tv.source.anime4k.domain.model

/** Ссылки на файлы озвучки по убыванию качества и Referer, без которого CDN отвечает 403. */
data class Anime4kStreams(
    val referer: String,
    val files: List<Anime4kFile>,
)
