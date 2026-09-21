package su.afk.yummy.tv.data.update.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("body") val body: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("assets") val assets: List<Asset> = emptyList(),
) {
    @Serializable
    data class Asset(
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )
}
