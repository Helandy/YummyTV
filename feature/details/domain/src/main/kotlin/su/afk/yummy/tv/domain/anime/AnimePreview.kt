package su.afk.yummy.tv.domain.anime

data class AnimePreview(
    val trailerEmbedUrl: String?,
    val trailerStreamUrl: String?,
    val description: String,
    val genres: List<String>,
    val year: Int?,
    val ageRating: String?,
    val type: String?,
    val views: Int?,
    val season: Int?,
    val screenshotUrls: List<String> = emptyList(),
)
