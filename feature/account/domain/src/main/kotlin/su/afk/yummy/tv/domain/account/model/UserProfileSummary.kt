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

data class UserWatchTypeStat(
    val id: Int,
    val alias: String,
    val title: String,
    val shortName: String,
    val spentSeconds: Long,
)

data class UserWatchHistoryDay(
    val dateSeconds: Long,
    val durationSeconds: Long,
    val episodeCount: Int,
)

data class UserProfileCounts(
    val watching: Int = 0,
    val planned: Int = 0,
    val completed: Int = 0,
    val dropped: Int = 0,
    val postponed: Int = 0,
    val favorite: Int = 0,
)

data class UserSocialCounts(
    val friends: Int = 0,
    val reviews: Int = 0,
    val comments: Int = 0,
    val posts: Int = 0,
    val collections: Int = 0,
)

enum class UserProfileSex {
    UNKNOWN,
    MALE,
    FEMALE,
}
