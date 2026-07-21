package su.afk.yummy.tv.domain.account.model

data class UserSearchItem(
    val id: Int,
    val nickname: String,
    val avatarUrl: String?,
    val lastOnlineSeconds: Long,
)
