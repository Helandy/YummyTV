package su.afk.yummy.tv.feature.posts.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PostsDestination : NavKey

@Serializable
data class PostDetailsDestination(val postId: Int) : NavKey
