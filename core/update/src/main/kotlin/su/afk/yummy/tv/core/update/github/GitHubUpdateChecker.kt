package su.afk.yummy.tv.core.update.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

private val GITHUB_API = if (UpdateConfig.GITHUB_OWNER.isNotBlank() && UpdateConfig.GITHUB_REPO.isNotBlank()) {
    "https://api.github.com/repos/${UpdateConfig.GITHUB_OWNER}/${UpdateConfig.GITHUB_REPO}/releases/latest"
} else null

class GitHubUpdateChecker(private val client: HttpClient) {

    suspend fun getLatestRelease(): GitHubReleaseDto? {
        val url = GITHUB_API ?: return null
        val response: HttpResponse = client.get(url) {
            header("Accept", "application/vnd.github+json")
        }
        if (!response.status.isSuccess()) return null
        return response.body()
    }
}
