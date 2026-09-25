package su.afk.yummy.tv.feature.posts.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import javax.inject.Inject

class PostsNavigator @Inject constructor() : IPostsNavigator {
    override fun list(): NavKey = PostsDestination
    override fun details(postId: Int): NavKey = PostDetailsDestination(postId)
}
