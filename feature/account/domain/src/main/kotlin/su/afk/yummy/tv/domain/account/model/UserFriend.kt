package su.afk.yummy.tv.domain.account.model

data class UserFriend(
    val id: Int,
    val nickname: String,
    val avatarUrl: String?,
    val lastOnlineSeconds: Long,
    val status: String,
)
