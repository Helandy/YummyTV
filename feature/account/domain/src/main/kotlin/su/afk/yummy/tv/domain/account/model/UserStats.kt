package su.afk.yummy.tv.domain.account.model

data class UserStats(
    val genres: List<UserGenreStat> = emptyList(),
    val ratings: List<UserRatingStat> = emptyList(),
    val lists: List<UserListWatchStat> = emptyList(),
    val types: List<UserAnimeTypeStat> = emptyList(),
)
