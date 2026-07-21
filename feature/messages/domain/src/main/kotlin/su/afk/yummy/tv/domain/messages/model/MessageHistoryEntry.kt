package su.afk.yummy.tv.domain.messages.model

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
