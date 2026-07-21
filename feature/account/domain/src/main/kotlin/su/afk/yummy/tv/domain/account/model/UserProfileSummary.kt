package su.afk.yummy.tv.domain.account.model

data class UserProfileSummary(
    val userId: Int,
    val nickname: String = "",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val registerDateSeconds: Long = 0L,
    val birthDateSeconds: Long = 0L,
    val sex: UserProfileSex = UserProfileSex.UNKNOWN,
    val about: String = "",
    val watchTypes: List<UserWatchTypeStat> = emptyList(),
    val watchHistory: List<UserWatchHistoryDay> = emptyList(),
    val daysOnline: Int = 0,
    val counts: UserProfileCounts = UserProfileCounts(),
    val socialCounts: UserSocialCounts = UserSocialCounts(),
)
