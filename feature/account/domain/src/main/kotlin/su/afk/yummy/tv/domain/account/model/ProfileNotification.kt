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
)

/**
 * Сколько непрочитанных в этой выборке ленты — число для бейджа аккаунта. Личные сообщения
 * исключены так же, как в [totalUnreadCount].
 */
fun List<ProfileNotification>.unreadBadgeCount(): Int =
    count { !it.viewed && !it.type.equals(MESSAGE_NOTIFICATION_TYPE, ignoreCase = true) }
