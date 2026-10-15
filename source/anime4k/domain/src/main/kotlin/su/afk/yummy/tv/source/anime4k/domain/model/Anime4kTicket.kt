package su.afk.yummy.tv.source.anime4k.domain.model

/** Одноразовый тикет на выдачу ссылок: погашается не раньше [notBeforeSec] после выдачи и до [expiresInSec]. */
data class Anime4kTicket(
    val value: String,
    val notBeforeSec: Int,
    val expiresInSec: Int,
)
