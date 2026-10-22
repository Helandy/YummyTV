package su.afk.yummy.tv.feature.comments

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.model.comments.CommentTargetType

interface ICommentsNavigator {
    /** [totalCount] — число комментариев (с ответами), если оно уже известно вызывающему экрану. */
    fun getCommentsDest(targetType: CommentTargetType, targetId: Int, totalCount: Int? = null): NavKey

    fun getAnimeCommentsDest(animeId: Int, totalCount: Int? = null): NavKey =
        getCommentsDest(CommentTargetType.ANIME, animeId, totalCount)
}
