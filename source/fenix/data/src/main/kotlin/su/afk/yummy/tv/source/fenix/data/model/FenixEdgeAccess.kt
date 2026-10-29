package su.afk.yummy.tv.source.fenix.data.model

/** Запасной медиа-хост страницы Fenix: [origin] без завершающего `/` и [token] для параметра `edge`. */
internal data class FenixEdgeAccess(
    val origin: String,
    val token: String,
)
