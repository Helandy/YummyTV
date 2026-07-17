package su.afk.yummy.tv.feature.bloggers.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class BloggerVideosDestination(val animeId: Int? = null) : NavKey

@Serializable
data class BloggerDetailsDestination(val bloggerId: Int) : NavKey

@Serializable
data class BloggerVideoDetailsDestination(val videoId: Int) : NavKey
