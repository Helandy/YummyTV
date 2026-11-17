package su.afk.yummy.tv.data.schedule.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import su.afk.yummy.tv.core.network.yani.YANI_BASE_URL
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.network.yani.YaniMirrorFallback
import su.afk.yummy.tv.data.schedule.dto.YaniScheduleResponseDto

/** Расписание yani; при недоступности yani читает копию на сервере YummyTV ([YaniMirrorFallback]). */
class YaniScheduleApi(
    private val clientProvider: YaniHttpClientProvider,
    private val fallback: YaniMirrorFallback,
) {
    suspend fun getSchedule(): YaniScheduleResponseDto = fallback.call(
        primary = { clientProvider.get().get("$YANI_BASE_URL/anime/schedule").body() },
        mirror = { fallback.mirrorGet("/yani/anime/schedule").body() },
    )
}
