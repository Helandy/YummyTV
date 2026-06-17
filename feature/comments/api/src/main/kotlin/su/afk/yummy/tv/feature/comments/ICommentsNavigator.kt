package su.afk.yummy.tv.feature.comments

import androidx.navigation3.runtime.NavKey

interface ICommentsNavigator {
    fun getAnimeCommentsDest(animeId: Int): NavKey
}
