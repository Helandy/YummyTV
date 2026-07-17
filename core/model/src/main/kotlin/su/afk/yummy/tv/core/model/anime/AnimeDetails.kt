package su.afk.yummy.tv.core.model.anime

data class AnimeDetails(
    val id: Int,
    val animeUrl: String,
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
