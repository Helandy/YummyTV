package su.afk.yummy.tv.source.anime4k.domain.model

/** Серия, у которой на сервере есть 4K-озвучки. [number] и [season] строками: бывают «01» и «1». */
data class Anime4kEpisode(
    val number: String,
    val season: String,
    /** Названия озвучек (команд); ссылки на файлы здесь нет, они выдаются только после рекламы. */
    val dubbings: List<String>,
)
