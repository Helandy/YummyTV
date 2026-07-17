package su.afk.yummy.tv.data.messages.network

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.data.messages.dto.YaniBanUserBodyDto
import su.afk.yummy.tv.data.messages.dto.YaniBooleanResponseDto
import su.afk.yummy.tv.data.messages.dto.YaniDeleteMessageBodyDto
import su.afk.yummy.tv.data.messages.dto.YaniDialogsResponseDto
import su.afk.yummy.tv.data.messages.dto.YaniEditMessageBodyDto
import su.afk.yummy.tv.data.messages.dto.YaniMessageHistoryResponseDto
import su.afk.yummy.tv.data.messages.dto.YaniMessageResponseDto
import su.afk.yummy.tv.data.messages.dto.YaniMessagesResponseDto
import su.afk.yummy.tv.data.messages.dto.YaniReadMessagesResponseDto
import su.afk.yummy.tv.data.messages.dto.YaniSendMessageBodyDto
import su.afk.yummy.tv.data.messages.dto.YaniUnbanUserBodyDto
import javax.inject.Inject

class YaniMessagesApi @Inject constructor(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun dialogs(limit: Int, offset: Int, needUserId: Int?): YaniDialogsResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/dialogs") {
            parameter("limit", limit)
            parameter("offset", offset)
            needUserId?.let { parameter("need_uid", it) }
        }.body()

    suspend fun messages(userId: Int, limit: Int, startFrom: Int): YaniMessagesResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/dialogs/$userId/messages") {
            parameter("limit", limit)
            parameter("start_from", startFrom)
        }.body()

    suspend fun sendMessage(userId: Int, text: String): YaniMessageResponseDto =
        clientProvider.get().post("$YANI_BASE_URL/dialogs/$userId/messages") {
            contentType(ContentType.Application.Json)
            setBody(YaniSendMessageBodyDto(message = text))
        }.body()

    suspend fun markRead(userId: Int): YaniReadMessagesResponseDto =
        clientProvider.get().post("$YANI_BASE_URL/dialogs/$userId/messages/read").body()

    suspend fun editMessage(messageId: Int, text: String): YaniMessageResponseDto =
        clientProvider.get().put("$YANI_BASE_URL/dialogs/messages/$messageId") {
            contentType(ContentType.Application.Json)
            setBody(YaniEditMessageBodyDto(reasonEdition = "", newText = text))
        }.body()

    suspend fun deleteMessage(messageId: Int): YaniMessageResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/dialogs/messages/$messageId") {
            contentType(ContentType.Application.Json)
            setBody(YaniDeleteMessageBodyDto())
        }.body()

    suspend fun restoreMessage(messageId: Int): YaniMessageResponseDto =
        clientProvider.get().post("$YANI_BASE_URL/dialogs/messages/$messageId/restore").body()

    suspend fun messageHistory(messageId: Int): YaniMessageHistoryResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/dialogs/messages/$messageId/history").body()

    suspend fun claimMessage(messageId: Int): YaniBooleanResponseDto =
        clientProvider.get().post("$YANI_BASE_URL/dialogs/messages/$messageId/claim").body()

    suspend fun banUser(userId: Int): YaniBooleanResponseDto =
        clientProvider.get().post("$YANI_BASE_URL/dialogs/$userId/ban") {
            contentType(ContentType.Application.Json)
            setBody(YaniBanUserBodyDto(userId = userId))
        }.body()

    suspend fun unbanUser(userId: Int): YaniBooleanResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/dialogs/$userId/ban") {
            contentType(ContentType.Application.Json)
            setBody(YaniUnbanUserBodyDto(userId = userId))
        }.body()
}
