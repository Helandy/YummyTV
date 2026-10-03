package su.afk.yummy.tv.domain.comments.model

import su.afk.yummy.tv.core.model.comments.CommentTargetType

data class CommentTarget(
    val type: CommentTargetType,
    val id: Int,
)
