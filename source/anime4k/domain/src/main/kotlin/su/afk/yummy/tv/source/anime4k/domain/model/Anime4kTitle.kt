package su.afk.yummy.tv.source.anime4k.domain.model

/** 4K-версия тайтла на сервере. Ссылок на файлы не содержит: их отдаёт только `getStreams`. */
data class Anime4kTitle(
    val malId: Int,
    val episodes: List<Anime4kEpisode>,
)
