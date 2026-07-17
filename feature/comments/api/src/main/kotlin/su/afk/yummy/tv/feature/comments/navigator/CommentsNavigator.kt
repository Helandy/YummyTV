package su.afk.yummy.tv.feature.comments.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.feature.comments.ICommentsNavigator

class CommentsNavigator : ICommentsNavigator {
    override fun getCommentsDest(targetType: CommentTargetType, targetId: Int): NavKey =
        CommentsDestination(targetType.name, targetId)
}
