package su.afk.yummy.tv.domain.anime

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

data class AnimePoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)

data class AnimeRating(
    val average: Double?,
    val counters: Int?,
    val kinopoisk: Double?,
    val shikimori: Double?,
    val myAnimeList: Double?,
)

data class AnimeGenre(
    val id: Int?,
    val title: String,
)

data class AnimePerson(
    val id: Int?,
    val title: String,
)

data class AnimeStudio(
    val id: Int?,
    val title: String,
)

data class AnimeViewingOrderItem(
    val animeId: Int,
    val title: String,
    val relation: String?,
    val type: String?,
    val episodesCount: Int?,
    val poster: AnimePoster?,
    val year: Int?,
    val rating: Double?,
)

data class AnimeEpisodes(
    val count: Int?,
    val aired: Int?,
    val nextDateEpochSeconds: Long?,
    val prevDateEpochSeconds: Long?,
)

data class AnimeScreenshot(
    val id: Int?,
    val episode: String?,
    val small: String?,
    val full: String?,
)
