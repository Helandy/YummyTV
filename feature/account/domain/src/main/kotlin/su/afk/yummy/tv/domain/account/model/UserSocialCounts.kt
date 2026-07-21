package su.afk.yummy.tv.domain.account.model

data class UserSocialCounts(
    val friends: Int = 0,
    val reviews: Int = 0,
    val comments: Int = 0,
    val posts: Int = 0,
    val collections: Int = 0,
)
