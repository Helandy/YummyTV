package su.afk.yummy.tv.domain.account.model

data class UserAnimeListItem(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val poster: UserAnimePoster? = null,
    val rating: Double?,
    val year: Int?,
    val list: UserAnimeList?,
    val isFavorite: Boolean,
    val updatedAtSeconds: Long? = null,
    val userRating: Int? = null,
)
