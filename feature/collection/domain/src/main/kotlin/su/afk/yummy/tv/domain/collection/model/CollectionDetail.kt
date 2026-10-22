package su.afk.yummy.tv.domain.collection.model

data class CollectionDetail(
    val id: Int,
    val ownerId: Int,
    val ownerNickname: String = "",
    val ownerAvatarUrl: String? = null,
    val commentsCount: Int = 0,
    val title: String,
    val description: String,
    val isPublic: Boolean,
    val views: Int,
    val posterUrl: String?,
    val likesCount: Int,
    val dislikesCount: Int,
    val vote: CollectionVote,
    val animes: List<CollectionAnimeItem>,
)
