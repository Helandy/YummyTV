package su.afk.yummy.tv.feature.comments

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.domain.comments.model.CommentTargetType

interface ICommentsNavigator {
    fun getCommentsDest(targetType: CommentTargetType, targetId: Int): NavKey

    fun getAnimeCommentsDest(animeId: Int): NavKey =
        getCommentsDest(CommentTargetType.ANIME, animeId)
}
