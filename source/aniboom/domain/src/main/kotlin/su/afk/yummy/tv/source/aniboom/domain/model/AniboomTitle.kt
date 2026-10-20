package su.afk.yummy.tv.source.aniboom.domain.model

/** Тайтл, который сервер нашёл в AniBoom: id в эмбеде, озвучки и сколько серий уже доступно. */
data class AniboomTitle(
    val aniboomId: String,
    val episodesAvailable: Int,
    val dubbings: List<AniboomDubbing>,
)
