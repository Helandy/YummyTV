package su.afk.yummy.tv.domain.account.model

data class NotificationCount(
    val type: String,
    val count: Int,
)

/**
 * Сумма непрочитанного для бейджа аккаунта. Личные сообщения исключены: у них свой счётчик в
 * диалогах, и во вкладке «Уведомления» они не показываются.
 */
fun List<NotificationCount>.totalUnreadCount(): Int =
    filterNot { it.type.equals(MESSAGE_NOTIFICATION_TYPE, ignoreCase = true) }.sumOf { it.count }

/** Личные сообщения: у них свой счётчик в диалогах, в бейдж аккаунта они не входят. */
internal const val MESSAGE_NOTIFICATION_TYPE = "message"
