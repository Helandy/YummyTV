package su.afk.yummy.tv.data.home.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import su.afk.yummy.tv.core.network.yani.YANI_BASE_URL
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniLastWatchesDto

class YaniHomeApi(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun getFeed(): YaniFeedDto =
        clientProvider.get().get("$YANI_BASE_URL/feed").body()

    suspend fun getLastWatches(): YaniLastWatchesDto =
        clientProvider.get().get("$YANI_BASE_URL/video/last-watches").body()
}
