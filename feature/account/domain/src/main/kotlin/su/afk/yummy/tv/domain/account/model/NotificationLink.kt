package su.afk.yummy.tv.domain.account.model

/** Ссылка на аниме из текста уведомления: [label] — название как в тексте, [slug] — идентификатор в каталоге. */
data class NotificationLink(
    val label: String,
    val slug: String,
)
