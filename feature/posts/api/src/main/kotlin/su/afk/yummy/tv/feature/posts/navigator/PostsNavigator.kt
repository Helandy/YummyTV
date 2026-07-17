package su.afk.yummy.tv.feature.posts.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.posts.IPostsNavigator

class PostsNavigator : IPostsNavigator {
    override fun list(): NavKey = PostsDestination
    override fun details(postId: Int): NavKey = PostDetailsDestination(postId)
}
