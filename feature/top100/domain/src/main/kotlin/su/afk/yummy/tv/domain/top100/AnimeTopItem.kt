package su.afk.yummy.tv.domain.top100

data class AnimeTopItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val blockedIn: List<String>,
)

enum class AnimeTopType(val apiValue: String) {
    TV("tv"),
    MOVIE("movie"),
    ONA("ona"),
}
