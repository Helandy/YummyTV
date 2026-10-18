package su.afk.yummy.tv.feature.account.userprofile.mapper

import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.feature.account.userprofile.model.UserProfileCommentPreview
import su.afk.yummy.tv.feature.account.userprofile.utils.commentReplyTo
import su.afk.yummy.tv.feature.account.userprofile.utils.toCommentPreviewText

internal fun Comment.toUserProfileCommentPreview(): UserProfileCommentPreview =
    UserProfileCommentPreview(
        id = id,
        text = text.toCommentPreviewText(),
        createdAtEpochSeconds = createdAtEpochSeconds,
        likes = likes,
        dislikes = dislikes,
        replyTo = text.commentReplyTo(),
        animeId = item?.animeId,
        animeTitle = item?.title,
        animePosterUrl = item?.posterUrl,
    )
