package su.afk.yummy.tv.domain.home.model

/** Новая серия тайтла из списков пользователя, о которой стоит показать локальный пуш. */
data class LibraryEpisodePush(
    val animeId: Int,
    val title: String,
    val episodeNumber: Int,
    val airedAtSeconds: Long,
)
