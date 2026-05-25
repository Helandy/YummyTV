package su.afk.yummy.tv.domain.account

data class YaniAccount(
    val id: Int,
    val nickname: String,
    val avatarUrl: String? = null,
)
