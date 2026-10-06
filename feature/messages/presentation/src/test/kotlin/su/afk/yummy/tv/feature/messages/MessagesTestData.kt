package su.afk.yummy.tv.feature.messages

import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.model.DialogSummary

internal fun chatMessage(
    id: Int,
    from: Int = PEER_ID,
    to: Int = ME_ID,
    text: String = "message $id",
    isRead: Boolean = true,
    isDeleted: Boolean = false,
    date: Long = id.toLong(),
) = ChatMessage(
    id = id,
    text = text,
    dateSeconds = date,
    fromUserId = from,
    toUserId = to,
    nickname = "user$from",
    avatarUrl = null,
    roles = emptyList(),
    isRead = isRead,
    isDeleted = isDeleted,
    deletedByUserId = null,
    isEdited = false,
    editedByUserId = null,
    reply = null,
)

internal fun dialog(userId: Int = PEER_ID, isBanned: Boolean = false) = DialogSummary(
    userId = userId,
    nickname = "user$userId",
    avatarUrl = null,
    roles = emptyList(),
    isBanned = isBanned,
    lastMessage = "",
    unreadCount = 0,
    dateSeconds = 0L,
    lastOnlineSeconds = 0L,
)

internal const val ME_ID = 1
internal const val PEER_ID = 2
