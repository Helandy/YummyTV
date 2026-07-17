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

data class MessageReply(
    val messageId: Int?,
    val text: String,
    val userId: Int?,
    val nickname: String?,
    val avatarUrl: String?,
)

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

enum class MessageHistoryChangeType {
    ADD,
    DELETE,
    EDIT,
    RESTORE,
    UNKNOWN,
}

data class MessageHistoryEntry(
    val userId: Int,
    val nickname: String,
    val avatarUrl: String?,
    val roles: List<String>,
    val dateSeconds: Long,
    val oldText: String,
    val newText: String,
    val changeType: MessageHistoryChangeType,
)
