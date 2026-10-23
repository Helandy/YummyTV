package su.afk.yummy.tv.source.anilibria.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaReleaseDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaSearchItemDto
import java.io.IOException
import javax.inject.Inject

/** Публичный API AniLibria (`aniliberty.top`): поиск релиза и серии с HLS-ссылками. Авторизация не нужна. */
internal class AniLibriaApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    suspend fun search(query: String): List<AniLibriaSearchItemDto> {
        val response = client.get("$BASE/app/search/releases") {
            parameter("query", query)
            parameter("include", SEARCH_FIELDS)
        }
        if (!response.status.isSuccess()) throw IOException("AniLibria API: HTTP ${response.status.value}")
        return response.body()
    }

    /** `null` при 404: релиза нет. */
    suspend fun getRelease(releaseId: Int): AniLibriaReleaseDto? {
        val response = client.get("$BASE/anime/releases/$releaseId") {
            parameter("include", RELEASE_FIELDS)
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("AniLibria API: HTTP ${response.status.value}")
        return response.body()
    }

    private companion object {
        const val BASE = "https://aniliberty.top/api/v1"
        const val SEARCH_FIELDS = "id,mal.id,name.main,name.english,name.alternative,year," +
            "is_blocked_by_geo,is_blocked_by_copyrights"
        const val RELEASE_FIELDS = "id,mal.id,name.main,is_blocked_by_geo,is_blocked_by_copyrights," +
            "episodes.ordinal,episodes.name,episodes.duration,episodes.opening," +
            "episodes.hls_480,episodes.hls_720,episodes.hls_1080"
    }
}
