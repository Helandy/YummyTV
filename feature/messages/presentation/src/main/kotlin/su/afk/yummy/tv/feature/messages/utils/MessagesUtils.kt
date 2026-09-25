package su.afk.yummy.tv.feature.messages.utils

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.domain.messages.model.GLOBAL_CHAT_USER_ID

/** Синтетическая запись общего чата. Имя/подпись подставляет UI по [GLOBAL_CHAT_USER_ID]. */
internal val GLOBAL_CHAT_SUMMARY = DialogSummary(
    userId = GLOBAL_CHAT_USER_ID,
    nickname = "",
    avatarUrl = null,
    roles = emptyList(),
    isBanned = false,
    lastMessage = "",
    unreadCount = 0,
    dateSeconds = 0,
    lastOnlineSeconds = 0,
)

/** Сливает сообщения по id (входящие побеждают) и сортирует по времени. */
internal fun mergeMessages(
    current: List<ChatMessage>,
    incoming: List<ChatMessage>,
): ImmutableList<ChatMessage> = buildMap {
    current.forEach { put(it.id, it) }
    incoming.forEach { put(it.id, it) }
}.values.sortedWith(compareBy(ChatMessage::dateSeconds, ChatMessage::id)).toImmutableList()
