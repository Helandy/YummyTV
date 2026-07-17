package su.afk.yummy.tv.data.messages.repository

import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.messages.dto.YaniDialogDto
import su.afk.yummy.tv.data.messages.dto.YaniMessageAvatarDto
import su.afk.yummy.tv.data.messages.dto.YaniMessageDto
import su.afk.yummy.tv.data.messages.dto.YaniMessageHistoryEntryDto
import su.afk.yummy.tv.data.messages.network.YaniMessagesApi
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.domain.messages.model.MessageHistoryChangeType
import su.afk.yummy.tv.domain.messages.model.MessageHistoryEntry
import su.afk.yummy.tv.domain.messages.model.MessageReply
import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

class YaniMessagesRepository @Inject constructor(
    private val api: YaniMessagesApi,
) : MessagesRepository {
    override suspend fun dialogs(limit: Int, offset: Int, needUserId: Int?) =
        api.dialogs(limit, offset, needUserId).response.dialogs
            .filter { it.userId > 0 }
            .map(YaniDialogDto::domain)

    override suspend fun messages(userId: Int, limit: Int, startFrom: Int) =
        api.messages(userId, limit, startFrom).response
            .filter { it.id > 0 }
            .map(YaniMessageDto::domain)

    override suspend fun sendMessage(userId: Int, text: String) =
        api.sendMessage(userId, text).response.domain()

    override suspend fun markRead(userId: Int) = api.markRead(userId).response.ok

    override suspend fun editMessage(messageId: Int, text: String) =
        api.editMessage(messageId, text).response.domain()

    override suspend fun deleteMessage(messageId: Int) =
        api.deleteMessage(messageId).response.domain()

    override suspend fun restoreMessage(messageId: Int) =
        api.restoreMessage(messageId).response.domain()

    override suspend fun messageHistory(messageId: Int) =
        api.messageHistory(messageId).response.map(YaniMessageHistoryEntryDto::domain)

    override suspend fun claimMessage(messageId: Int) = api.claimMessage(messageId).response

    override suspend fun setUserBanned(userId: Int, banned: Boolean) =
        if (banned) api.banUser(userId).response else api.unbanUser(userId).response
}

private fun YaniDialogDto.domain() = DialogSummary(
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

private fun YaniMessageDto.domain() = ChatMessage(
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

private fun YaniMessageAvatarDto?.bestUrl(): String? =
    (this?.full ?: this?.big ?: this?.small)?.toHttpsUrl()

private fun YaniMessageHistoryEntryDto.domain() = MessageHistoryEntry(
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
