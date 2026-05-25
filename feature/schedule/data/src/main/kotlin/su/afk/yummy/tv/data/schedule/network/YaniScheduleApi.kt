package su.afk.yummy.tv.data.schedule.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.schedule.dto.YaniScheduleResponseDto

class YaniScheduleApi(
    private val client: HttpClient,
) {
    suspend fun getSchedule(): YaniScheduleResponseDto =
        client.get("$YANI_BASE_URL/anime/schedule").body()
}
