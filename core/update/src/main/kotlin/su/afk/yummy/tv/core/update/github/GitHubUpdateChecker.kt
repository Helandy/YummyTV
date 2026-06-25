package su.afk.yummy.tv.core.update.github

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.YaniHttpClientProvider

private val GITHUB_API = if (UpdateConfig.GITHUB_OWNER.isNotBlank() && UpdateConfig.GITHUB_REPO.isNotBlank()) {
    "https://api.github.com/repos/${UpdateConfig.GITHUB_OWNER}/${UpdateConfig.GITHUB_REPO}/releases/latest"
} else null

class GitHubUpdateChecker(private val clientProvider: YaniHttpClientProvider) {

    suspend fun getLatestRelease(): GitHubReleaseDto? {
        val url = GITHUB_API ?: return null
        val response: HttpResponse = clientProvider.get().get(url) {
            header("Accept", "application/vnd.github+json")
        }
        if (!response.status.isSuccess()) return null
        return response.body()
    }
}
