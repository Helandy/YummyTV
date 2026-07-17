package su.afk.yummy.tv.domain.collection.model

data class CreateCollectionRequest(
    val title: String,
    val description: String,
    val isPublic: Boolean,
)
