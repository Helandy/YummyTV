package su.afk.yummy.tv.data.schedule.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.data.schedule.dto.YaniScheduleResponseDto

class YaniScheduleApi(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun getSchedule(): YaniScheduleResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/schedule").body()
}
