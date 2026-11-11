package su.afk.yummy.tv.source.torrent.domain.model

/** Адрес потока на TorrServer и заголовки, с которыми его нужно запрашивать (авторизация, если задана). */
data class TorrentStream(
    val url: String,
    val headers: Map<String, String>,
)
