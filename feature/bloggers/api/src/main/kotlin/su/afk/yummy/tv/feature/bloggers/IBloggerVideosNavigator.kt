package su.afk.yummy.tv.feature.bloggers

import androidx.navigation3.runtime.NavKey

interface IBloggerVideosNavigator {
    fun feed(): NavKey
    fun anime(animeId: Int): NavKey
    fun blogger(bloggerId: Int): NavKey
    fun video(videoId: Int): NavKey
}
