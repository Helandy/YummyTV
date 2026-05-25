package su.afk.yummy.tv.domain.account.model

data class YaniAccount(
    val id: Int,
    val nickname: String,
    val avatarUrl: String? = null,
)
