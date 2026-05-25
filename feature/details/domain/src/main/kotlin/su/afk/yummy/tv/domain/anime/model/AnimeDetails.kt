package su.afk.yummy.tv.domain.anime.model

data class AnimeDetails(
    val id: Int,
    val title: String,
    val description: String,
    val poster: AnimePoster?,
    val rating: AnimeRating,
    val genres: List<AnimeGenre>,
    val year: Int?,
    val ageRating: String?,
    val views: Int?,
    val status: String?,
    val type: String?,
    val episodes: AnimeEpisodes?,
    val otherTitles: List<String>,
    val creators: List<AnimePerson>,
    val studios: List<AnimeStudio>,
    val viewingOrder: List<AnimeViewingOrderItem>,
    val screenshots: List<AnimeScreenshot>,
)
