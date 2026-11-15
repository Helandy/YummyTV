package su.afk.yummy.tv.data.comments.network

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.data.comments.dto.YaniCommentErrorDto

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Выполняет запрос и, если сервер отклонил его с текстом причины в теле (`{"error": "..."}`),
 * бросает [YaniCommentsServerException] с этим текстом — его покажет тост. Ответы без
 * разбираемого тела остаются исходным исключением.
 */
internal suspend inline fun <T> withServerMessage(block: () -> T): T =
    try {
        block()
    } catch (e: ResponseException) {
        val message = e.serverMessage()
        if (message == null) throw e else throw YaniCommentsServerException(message, e)
    }

private suspend fun ResponseException.serverMessage(): String? =
    runSuspendCatching { errorJson.decodeFromString<YaniCommentErrorDto>(response.bodyAsText()) }
        .getOrNull()
        ?.error
        ?.takeIf { it.isNotBlank() }
