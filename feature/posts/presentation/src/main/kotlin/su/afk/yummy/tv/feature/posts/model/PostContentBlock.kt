package su.afk.yummy.tv.feature.posts.model

sealed interface PostContentBlock {
    data class Text(val value: String) : PostContentBlock
    data class Image(val url: String, val description: String) : PostContentBlock
}
