package su.afk.yummy.tv.data.library.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import su.afk.yummy.tv.core.network.yani.YANI_BASE_URL
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.data.library.dto.YaniTransferLookupAnimeDto
import su.afk.yummy.tv.data.library.dto.YaniTransferLookupResponseDto
import su.afk.yummy.tv.data.library.dto.YaniTransferUserAnimeDto
import su.afk.yummy.tv.data.library.dto.YaniTransferUserListResponseDto
import javax.inject.Inject

/**
 * Сетевые запросы импорта и экспорта списка аниме: чтение всех списков пользователя одним GET
 * (включая `remote_ids`, статус и оценку) и пакетный поиск тайтлов по `shikimori_ids` / `mal_ids` / `ids`.
 */
class YaniListTransferApi @Inject constructor(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun getAllUserLists(userId: Int): List<YaniTransferUserAnimeDto> =
        clientProvider.get()
            .get("$YANI_BASE_URL/users/$userId/lists")
            .body<YaniTransferUserListResponseDto>()
            .response

    suspend fun lookupByShikimoriIds(
        ids: List<Int>,
        limit: Int,
        offset: Int,
    ): List<YaniTransferLookupAnimeDto> =
        lookupByIdParam(paramName = "shikimori_ids", ids = ids, limit = limit, offset = offset)

    suspend fun lookupByMalIds(
        ids: List<Int>,
        limit: Int,
        offset: Int,
    ): List<YaniTransferLookupAnimeDto> =
        lookupByIdParam(paramName = "mal_ids", ids = ids, limit = limit, offset = offset)

    suspend fun lookupByAnimeIds(
        ids: List<Int>,
        limit: Int,
        offset: Int,
    ): List<YaniTransferLookupAnimeDto> =
        lookupByIdParam(paramName = "ids", ids = ids, limit = limit, offset = offset)

    private suspend fun lookupByIdParam(
        paramName: String,
        ids: List<Int>,
        limit: Int,
        offset: Int,
    ): List<YaniTransferLookupAnimeDto> {
        if (ids.isEmpty()) return emptyList()
        return clientProvider.get().get("$YANI_BASE_URL/anime") {
            parameter(paramName, ids.joinToString(","))
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniTransferLookupResponseDto>().response
    }
}
