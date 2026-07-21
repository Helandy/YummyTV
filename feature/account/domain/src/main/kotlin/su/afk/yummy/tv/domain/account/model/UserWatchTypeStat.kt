package su.afk.yummy.tv.domain.account.model

data class UserWatchTypeStat(
    val id: Int,
    val alias: String,
    val title: String,
    val shortName: String,
    val spentSeconds: Long,
)
