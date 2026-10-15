package su.afk.yummy.tv.domain.account.model

data class ProfileNotification(
    val id: Int,
    val dateSeconds: Long,
    val title: String,
    val text: String,
    val clickUri: String,
    val type: String,
    val subType: String,
    val viewed: Boolean,
    val objectId: Int?,
    val animeSlug: String? = null,
    val isNewEpisode: Boolean = false,
    val links: List<NotificationLink> = emptyList(),
)

/** У уведомления есть собственное действие по клику (переход на аниме), разворачивать его не нужно. */
val ProfileNotification.hasOpenAction: Boolean
    get() = isNewEpisode && animeSlug != null

/** Клик по уведомлению разворачивает его: полный текст и ссылки на аниме. */
val ProfileNotification.isExpandable: Boolean
    get() = !hasOpenAction && (text.isNotBlank() || links.isNotEmpty())

/**
 * Сколько непрочитанных в этой выборке ленты — число для бейджа аккаунта. Личные сообщения
 * исключены так же, как в [totalUnreadCount].
 */
fun List<ProfileNotification>.unreadBadgeCount(): Int =
    count { !it.viewed && !it.type.equals(MESSAGE_NOTIFICATION_TYPE, ignoreCase = true) }
