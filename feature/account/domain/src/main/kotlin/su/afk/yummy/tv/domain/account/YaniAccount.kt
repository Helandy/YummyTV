package su.afk.yummy.tv.domain.account

data class YaniAccount(
    val id: Int,
    val nickname: String,
    val avatarUrl: String? = null,
)

enum class UserAnimeList(val id: Int) {
    WATCHING(0),
    PLANNED(1),
    COMPLETED(2),
    DROPPED(3),
    POSTPONED(5),
}

data class UserAnimeListItem(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val year: Int?,
    val list: UserAnimeList?,
    val isFavorite: Boolean,
)

data class RemoteWatchState(
    val videoId: Int,
    val timeSeconds: Int,
    val durationSeconds: Int,
)

data class AnimeRatingSummary(
    val distribution: List<AnimeRatingBucket> = emptyList(),
    val userRating: Int? = null,
)

data class AnimeRatingBucket(
    val rating: Int,
    val count: Int,
)

data class AnimeListStats(
    val counts: Map<Int, Int> = emptyMap(),
)

data class AnimeCollectionSummary(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val views: Int?,
)
