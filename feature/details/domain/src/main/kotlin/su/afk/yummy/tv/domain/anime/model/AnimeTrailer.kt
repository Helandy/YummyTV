package su.afk.yummy.tv.domain.anime.model

private val youtubeEmbedRegex = Regex("youtube\\.com/embed/([a-zA-Z0-9_-]+)")

data class AnimeTrailer(
    val iframeUrl: String,
) {
    val youtubeVideoId: String? = youtubeEmbedRegex.find(iframeUrl)?.groupValues?.getOrNull(1)
    val youtubeThumbnailUrl: String? = youtubeVideoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
    val youtubeWatchUrl: String? = youtubeVideoId?.let { "https://www.youtube.com/watch?v=$it" }
    val externalWatchUrl: String = youtubeWatchUrl ?: iframeUrl
}
