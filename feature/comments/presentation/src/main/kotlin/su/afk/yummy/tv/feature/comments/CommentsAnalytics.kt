package su.afk.yummy.tv.feature.comments

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentTarget
import su.afk.yummy.tv.domain.comments.model.CommentVote
import javax.inject.Inject

internal class CommentsAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    fun eventScreenOpened(target: CommentTarget, sort: CommentSort) =
        eventWithTarget(EVENT_SCREEN_OPENED, target, sort)

    fun eventRetrySelected(target: CommentTarget, sort: CommentSort) =
        eventWithTarget(EVENT_RETRY_SELECTED, target, sort)

    fun eventRefreshSelected(target: CommentTarget, sort: CommentSort) =
        eventWithTarget(EVENT_REFRESH_SELECTED, target, sort)

    fun eventSortSelected(target: CommentTarget, sort: CommentSort) =
        eventWithTarget(EVENT_SORT_SELECTED, target, sort)

    fun eventAuthorSelected(target: CommentTarget, userId: Int) = tracker.track(
        EVENT_AUTHOR_SELECTED,
        target.params() + analyticsParamsOf(PARAM_USER_ID to userId),
    )

    fun eventReplySelected(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_REPLY_SELECTED, target, commentId)

    fun eventEditSelected(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_EDIT_SELECTED, target, commentId)

    fun eventDeleteSelected(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_DELETE_SELECTED, target, commentId)

    fun eventReportSelected(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_REPORT_SELECTED, target, commentId)

    fun eventCreated(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_CREATED, target, commentId)

    fun eventReplyCreated(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_REPLY_CREATED, target, commentId)

    fun eventUpdated(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_UPDATED, target, commentId)

    fun eventDeleted(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_DELETED, target, commentId)

    fun eventReported(target: CommentTarget, commentId: Int, reason: CommentReportReason) =
        tracker.track(
            EVENT_REPORTED,
            commentParams(
                target,
                commentId
            ) + analyticsParamsOf(PARAM_REASON to reason.name.lowercase()),
        )

    fun eventVoteChanged(target: CommentTarget, commentId: Int, vote: CommentVote) =
        tracker.track(
            EVENT_VOTE_CHANGED,
            commentParams(
                target,
                commentId
            ) + analyticsParamsOf(PARAM_VOTE to vote.name.lowercase()),
        )

    fun eventRepliesShown(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_REPLIES_SHOWN, target, commentId)

    fun eventRepliesHidden(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_REPLIES_HIDDEN, target, commentId)

    fun eventRepliesLoadMoreSelected(target: CommentTarget, commentId: Int) =
        eventWithComment(EVENT_REPLIES_LOAD_MORE_SELECTED, target, commentId)

    fun eventLoadError(target: CommentTarget, sort: CommentSort, throwable: Throwable) =
        tracker.reportError(
            groupIdentifier = ERROR_LOAD,
            message = "Comments load failed (${target.type.apiValue}/${target.id}, " +
                    "sort=${sort.name.lowercase()}): ${throwable.analyticsType()}",
            throwable = throwable,
        )

    fun eventRepliesLoadError(target: CommentTarget, commentId: Int, throwable: Throwable) =
        tracker.reportError(
            groupIdentifier = ERROR_REPLIES_LOAD,
            message = "Comment replies load failed (${target.type.apiValue}/${target.id}, " +
                    "comment_id=$commentId): ${throwable.analyticsType()}",
            throwable = throwable,
        )

    private fun eventWithTarget(event: String, target: CommentTarget, sort: CommentSort) =
        tracker.track(
            event,
            target.params() + analyticsParamsOf(PARAM_SORT to sort.name.lowercase())
        )

    private fun eventWithComment(event: String, target: CommentTarget, commentId: Int) =
        tracker.track(event, commentParams(target, commentId))

    private fun commentParams(target: CommentTarget, commentId: Int) =
        target.params() + analyticsParamsOf(PARAM_COMMENT_ID to commentId)

    private fun CommentTarget.params() = analyticsParamsOf(
        PARAM_TARGET_TYPE to type.apiValue,
        PARAM_TARGET_ID to id,
    )

    private fun Throwable.analyticsType(): String =
        this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

    internal companion object {
        private const val ERROR_LOAD = "comments_load_error"
        private const val ERROR_REPLIES_LOAD = "comments_replies_load_error"
        private const val PARAM_TARGET_TYPE = "target_type"
        private const val PARAM_TARGET_ID = "target_id"
        private const val PARAM_COMMENT_ID = "comment_id"
        private const val PARAM_REASON = "reason"
        private const val PARAM_SORT = "sort"
        private const val PARAM_USER_ID = "user_id"
        private const val PARAM_VOTE = "vote"

        const val EVENT_AUTHOR_SELECTED = "comments_author_selected"
        const val EVENT_CREATED = "comments_created"
        const val EVENT_DELETED = "comments_deleted"
        const val EVENT_DELETE_SELECTED = "comments_delete_selected"
        const val EVENT_EDIT_SELECTED = "comments_edit_selected"
        const val EVENT_REFRESH_SELECTED = "comments_refresh_selected"
        const val EVENT_REPLIES_HIDDEN = "comments_replies_hidden"
        const val EVENT_REPLIES_LOAD_MORE_SELECTED = "comments_replies_load_more_selected"
        const val EVENT_REPLIES_SHOWN = "comments_replies_shown"
        const val EVENT_REPORTED = "comments_reported"
        const val EVENT_REPORT_SELECTED = "comments_report_selected"
        const val EVENT_REPLY_CREATED = "comments_reply_created"
        const val EVENT_REPLY_SELECTED = "comments_reply_selected"
        const val EVENT_RETRY_SELECTED = "comments_retry_selected"
        const val EVENT_SCREEN_OPENED = "comments_screen"
        const val EVENT_SORT_SELECTED = "comments_sort_selected"
        const val EVENT_UPDATED = "comments_updated"
        const val EVENT_VOTE_CHANGED = "comments_vote_changed"
    }
}
