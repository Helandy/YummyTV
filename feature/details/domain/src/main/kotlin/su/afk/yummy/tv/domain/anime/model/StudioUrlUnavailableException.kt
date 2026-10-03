package su.afk.yummy.tv.domain.anime.model

/** У студии нет ссылки, по которой можно загрузить связанные тайтлы. */
class StudioUrlUnavailableException : Exception("Studio URL is unavailable")
