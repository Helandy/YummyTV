package su.afk.yummy.tv.data.messages.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniDialogsResponseDto(
    val response: YaniDialogsPayloadDto = YaniDialogsPayloadDto(),
)

@Serializable
data class YaniDialogsPayloadDto(val dialogs: List<YaniDialogDto> = emptyList())

@Serializable
data class YaniDialogDto(
    @SerialName("user_id") val userId: Int = 0,
    val nickname: String = "",
    val avatars: YaniMessageAvatarDto? = null,
    val roles: List<String> = emptyList(),
    val banned: Boolean = false,
    @SerialName("last_message") val lastMessage: String = "",
    @SerialName("unread_count") val unreadCount: Int = 0,
    val date: Long = 0,
    @SerialName("last_online") val lastOnline: Long = 0,
)

@Serializable
data class YaniMessagesResponseDto(val response: List<YaniMessageDto> = emptyList())

@Serializable
data class YaniMessageResponseDto(val response: YaniMessageDto = YaniMessageDto())

@Serializable
data class YaniMessageAvatarDto(
    val small: String? = null,
    val big: String? = null,
    val full: String? = null,
)

@Serializable
data class YaniMessageReplyUserDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatars: YaniMessageAvatarDto? = null,
)

@Serializable
data class YaniMessageDto(
    val id: Int = 0,
    val avatars: YaniMessageAvatarDto? = null,
    val roles: List<String> = emptyList(),
    val text: String = "",
    @SerialName("answer_to_id") val answerToId: Int? = null,
    val date: Long = 0,
    val deleted: Boolean = false,
    @SerialName("deleted_by_id") val deletedById: Int? = null,
    val edited: Boolean = false,
    @SerialName("edited_by_id") val editedById: Int? = null,
    @SerialName("from_id") val fromId: Int = 0,
    @SerialName("message_to_answer") val messageToAnswer: String? = null,
    val nickname: String = "",
    val read: Boolean = false,
    @SerialName("to_id") val toId: Int = 0,
    @SerialName("user_to_answer") val userToAnswer: YaniMessageReplyUserDto? = null,
)

@Serializable
data class YaniSendMessageBodyDto(
    @SerialName("answer_msg_id") val answerMessageId: Int = 0,
    val message: String,
)

@Serializable
data class YaniEditMessageBodyDto(
    @SerialName("reason_edition") val reasonEdition: String,
    @SerialName("new_text") val newText: String,
)

@Serializable
data class YaniReadMessagesResponseDto(
    val response: YaniReadMessagesPayloadDto = YaniReadMessagesPayloadDto(),
)

@Serializable
data class YaniReadMessagesPayloadDto(val ok: Boolean = false)

@Serializable
data class YaniDeleteMessageBodyDto(val reason: String = "")

@Serializable
data class YaniMessageHistoryResponseDto(
    val response: List<YaniMessageHistoryEntryDto> = emptyList(),
)

@Serializable
data class YaniMessageHistoryEntryDto(
    @SerialName("user_id") val userId: Int = 0,
    val nickname: String = "",
    val avatars: YaniMessageAvatarDto? = null,
    val roles: List<String> = emptyList(),
    val date: Long = 0,
    @SerialName("old_text") val oldText: String = "",
    @SerialName("new_text") val newText: String = "",
    @SerialName("change_type") val changeType: String = "",
)

@Serializable
data class YaniBooleanResponseDto(val response: Boolean = false)

@Serializable
data class YaniBanUserBodyDto(
    @SerialName("user_id") val userId: Int,
    val reason: String = "",
    val unbandelta: Int = 0,
)

@Serializable
data class YaniUnbanUserBodyDto(@SerialName("user_id") val userId: Int)
