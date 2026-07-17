package su.afk.yummy.tv.feature.bloggers.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator

class BloggerVideosNavigator : IBloggerVideosNavigator {
    override fun feed(): NavKey = BloggerVideosDestination()
    override fun anime(animeId: Int): NavKey = BloggerVideosDestination(animeId)
    override fun blogger(bloggerId: Int): NavKey = BloggerDetailsDestination(bloggerId)
    override fun video(videoId: Int): NavKey = BloggerVideoDetailsDestination(videoId)
}
