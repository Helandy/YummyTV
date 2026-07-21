package su.afk.yummy.tv.domain.messages.model

data class ChatMessage(
    val id: Int,
    val text: String,
    val dateSeconds: Long,
    val fromUserId: Int,
    val toUserId: Int,
    val nickname: String,
    val avatarUrl: String?,
    val roles: List<String>,
    val isRead: Boolean,
    val isDeleted: Boolean,
    val deletedByUserId: Int?,
    val isEdited: Boolean,
    val editedByUserId: Int?,
    val reply: MessageReply?,
)
