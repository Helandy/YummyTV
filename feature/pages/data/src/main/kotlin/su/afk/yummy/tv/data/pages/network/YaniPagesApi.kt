package su.afk.yummy.tv.data.pages.network

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import javax.inject.Inject

class YaniPagesApi @Inject constructor(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun getPage(type: String): String =
        clientProvider.get().get("$YANI_BASE_URL/pages/$type").bodyAsText()
}
