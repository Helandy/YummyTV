package su.afk.yummy.tv.data.library.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.data.library.dto.YaniWatchHistoryResponseDto
import javax.inject.Inject

class YaniWatchHistoryApi @Inject constructor(private val clientProvider: YaniHttpClientProvider) {
    suspend fun getPage(limit: Int, offset: Int): YaniWatchHistoryResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/video/watch-history") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
}
