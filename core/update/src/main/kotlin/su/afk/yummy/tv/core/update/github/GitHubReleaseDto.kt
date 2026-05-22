package su.afk.yummy.tv.core.update.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("body") val body: String? = null,
    @SerialName("assets") val assets: List<Asset> = emptyList(),
) {
    @Serializable
    data class Asset(
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )
}