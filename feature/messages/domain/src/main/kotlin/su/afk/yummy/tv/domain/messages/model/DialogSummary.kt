package su.afk.yummy.tv.domain.messages.model

data class DialogSummary(
    val userId: Int,
    val nickname: String,
    val avatarUrl: String?,
    val roles: List<String>,
    val isBanned: Boolean,
    val lastMessage: String,
    val unreadCount: Int,
    val dateSeconds: Long,
    val lastOnlineSeconds: Long,
)
