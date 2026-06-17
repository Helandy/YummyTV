package su.afk.yummy.tv.feature.comments.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.comments.ICommentsNavigator

class CommentsNavigator : ICommentsNavigator {
    override fun getAnimeCommentsDest(animeId: Int): NavKey =
        AnimeCommentsDestination(animeId)
}
