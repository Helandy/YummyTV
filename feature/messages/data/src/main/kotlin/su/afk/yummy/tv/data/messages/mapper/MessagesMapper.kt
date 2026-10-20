package su.afk.yummy.tv.data.messages.mapper

import su.afk.yummy.tv.core.utils.network.toHttpsUrlOrNull
import su.afk.yummy.tv.data.messages.dto.YaniDialogDto
import su.afk.yummy.tv.data.messages.dto.YaniMessageAvatarDto
import su.afk.yummy.tv.data.messages.dto.YaniMessageDto
import su.afk.yummy.tv.data.messages.dto.YaniMessageHistoryEntryDto
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.domain.messages.model.MessageHistoryChangeType
import su.afk.yummy.tv.domain.messages.model.MessageHistoryEntry
import su.afk.yummy.tv.domain.messages.model.MessageReply

internal fun YaniDialogDto.domain() = DialogSummary(
    userId = userId,
    nickname = nickname,
    avatarUrl = avatars.bestUrl(),
    roles = roles,
    isBanned = banned,
    lastMessage = lastMessage,
    unreadCount = unreadCount.coerceAtLeast(0),
    dateSeconds = date,
    lastOnlineSeconds = lastOnline,
)

internal fun YaniMessageDto.domain() = ChatMessage(
    id = id,
    text = text,
    dateSeconds = date,
    fromUserId = fromId,
    toUserId = toId,
    nickname = nickname,
    avatarUrl = avatars.bestUrl(),
    roles = roles,
    isRead = read,
    isDeleted = deleted,
    deletedByUserId = deletedById?.takeIf { it > 0 },
    isEdited = edited,
    editedByUserId = editedById?.takeIf { it > 0 },
    reply = messageToAnswer?.takeIf { it.isNotBlank() }?.let { replyText ->
        MessageReply(
            messageId = answerToId?.takeIf { it > 0 },
            text = replyText,
            userId = userToAnswer?.id?.takeIf { it > 0 },
            nickname = userToAnswer?.nickname?.takeIf { it.isNotBlank() },
            avatarUrl = userToAnswer?.avatars.bestUrl(),
        )
    },
)

/** Для пользователя без переписки yani отдаёт несуществующую заглушку `.../undefined.webp` — это не аватарка. */
private fun YaniMessageAvatarDto?.bestUrl(): String? =
    listOf(this?.full, this?.big, this?.small)
        .mapNotNull { it.toHttpsUrlOrNull() }
        .firstOrNull { MISSING_AVATAR_MARK !in it }

private const val MISSING_AVATAR_MARK = "/undefined."

internal fun YaniMessageHistoryEntryDto.domain() = MessageHistoryEntry(
    userId = userId,
    nickname = nickname,
    avatarUrl = avatars.bestUrl(),
    roles = roles,
    dateSeconds = date,
    oldText = oldText,
    newText = newText,
    changeType = when (changeType) {
        "add" -> MessageHistoryChangeType.ADD
        "delete" -> MessageHistoryChangeType.DELETE
        "edit" -> MessageHistoryChangeType.EDIT
        "restore" -> MessageHistoryChangeType.RESTORE
        else -> MessageHistoryChangeType.UNKNOWN
    },
)
