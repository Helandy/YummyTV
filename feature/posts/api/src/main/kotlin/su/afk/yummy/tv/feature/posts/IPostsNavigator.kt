package su.afk.yummy.tv.feature.posts

import androidx.navigation3.runtime.NavKey

interface IPostsNavigator {
    fun list(): NavKey
    fun details(postId: Int): NavKey
}
