package su.afk.yummy.tv.domain.messages.model

data class MessageReply(
    val messageId: Int?,
    val text: String,
    val userId: Int?,
    val nickname: String?,
    val avatarUrl: String?,
)
