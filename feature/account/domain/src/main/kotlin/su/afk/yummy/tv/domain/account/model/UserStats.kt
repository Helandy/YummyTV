package su.afk.yummy.tv.domain.account.model

data class UserStats(
    val genres: List<UserGenreStat> = emptyList(),
    val ratings: List<UserRatingStat> = emptyList(),
    val lists: List<UserListWatchStat> = emptyList(),
    val types: List<UserAnimeTypeStat> = emptyList(),
)

data class UserGenreStat(
    val id: Int,
    val title: String,
    val count: Int,
)

data class UserRatingStat(
    val rating: Int,
    val count: Int,
)

data class UserListWatchStat(
    val id: Int,
    val title: String,
    val href: String,
    val seconds: Long,
)

data class UserAnimeTypeStat(
    val id: Int,
    val title: String,
    val shortName: String,
    val count: Int,
)
