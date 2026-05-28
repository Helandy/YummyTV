package su.afk.yummy.tv.domain.account.model

data class ProfileNotification(
    val id: Int,
    val dateSeconds: Long,
    val title: String,
    val text: String,
    val clickUri: String,
    val type: String,
    val subType: String,
    val viewed: Boolean,
    val objectId: Int?,
    val animeSlug: String? = null,
    val isNewEpisode: Boolean = false,
)

data class NotificationCount(
    val type: String,
    val count: Int,
)
