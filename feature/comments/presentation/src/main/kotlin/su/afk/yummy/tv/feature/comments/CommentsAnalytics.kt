package su.afk.yummy.tv.feature.comments

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentVote
import javax.inject.Inject

internal class CommentsAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь открыл экран комментариев.
     *
     * Параметры: anime_id, sort.
     */
    fun eventScreenOpened(animeId: Int, sort: CommentSort) {
        eventWithAnime(EVENT_SCREEN_OPENED, animeId, sort)
    }

    /**
     * Пользователь повторил загрузку комментариев.
     *
     * Параметры: anime_id, sort.
     */
    fun eventRetrySelected(animeId: Int, sort: CommentSort) {
        eventWithAnime(EVENT_RETRY_SELECTED, animeId, sort)
    }

    /**
     * Пользователь обновил список комментариев.
     *
     * Параметры: anime_id, sort.
     */
    fun eventRefreshSelected(animeId: Int, sort: CommentSort) {
        eventWithAnime(EVENT_REFRESH_SELECTED, animeId, sort)
    }

    /**
     * Пользователь сменил сортировку комментариев.
     *
     * Параметры: anime_id, sort.
     */
    fun eventSortSelected(animeId: Int, sort: CommentSort) {
        eventWithAnime(EVENT_SORT_SELECTED, animeId, sort)
    }

    /**
     * Пользователь открыл автора комментария.
     *
     * Параметры: anime_id, user_id.
     */
    fun eventAuthorSelected(animeId: Int, userId: Int) {
        tracker.track(
            EVENT_AUTHOR_SELECTED,
            analyticsParamsOf(
                PARAM_ANIME_ID to animeId,
                PARAM_USER_ID to userId,
            ),
        )
    }

    /**
     * Пользователь начал отвечать на комментарий.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventReplySelected(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_REPLY_SELECTED, animeId, commentId)
    }

    /**
     * Пользователь начал редактировать комментарий.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventEditSelected(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_EDIT_SELECTED, animeId, commentId)
    }

    /**
     * Пользователь открыл подтверждение удаления комментария.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventDeleteSelected(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_DELETE_SELECTED, animeId, commentId)
    }

    /**
     * Пользователь открыл жалобу на комментарий.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventReportSelected(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_REPORT_SELECTED, animeId, commentId)
    }

    /**
     * Пользователь успешно создал комментарий.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventCreated(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_CREATED, animeId, commentId)
    }

    /**
     * Пользователь успешно создал ответ.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventReplyCreated(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_REPLY_CREATED, animeId, commentId)
    }

    /**
     * Пользователь успешно обновил комментарий.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventUpdated(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_UPDATED, animeId, commentId)
    }

    /**
     * Пользователь успешно удалил комментарий.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventDeleted(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_DELETED, animeId, commentId)
    }

    /**
     * Пользователь успешно отправил жалобу на комментарий.
     *
     * Параметры: anime_id, comment_id, reason.
     */
    fun eventReported(animeId: Int, commentId: Int, reason: CommentReportReason) {
        tracker.track(
            EVENT_REPORTED,
            commentParams(animeId, commentId) + analyticsParamsOf(
                PARAM_REASON to reason.analyticsValue(),
            ),
        )
    }

    /**
     * Пользователь изменил голос за комментарий.
     *
     * Параметры: anime_id, comment_id, vote.
     */
    fun eventVoteChanged(animeId: Int, commentId: Int, vote: CommentVote) {
        tracker.track(
            EVENT_VOTE_CHANGED,
            commentParams(animeId, commentId) + analyticsParamsOf(
                PARAM_VOTE to vote.analyticsValue(),
            ),
        )
    }

    /**
     * Пользователь раскрыл ответы к комментарию.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventRepliesShown(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_REPLIES_SHOWN, animeId, commentId)
    }

    /**
     * Пользователь скрыл ответы к комментарию.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventRepliesHidden(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_REPLIES_HIDDEN, animeId, commentId)
    }

    /**
     * Пользователь запросил следующую страницу ответов.
     *
     * Параметры: anime_id, comment_id.
     */
    fun eventRepliesLoadMoreSelected(animeId: Int, commentId: Int) {
        eventWithComment(EVENT_REPLIES_LOAD_MORE_SELECTED, animeId, commentId)
    }

    /**
     * Ошибка загрузки комментариев.
     */
    fun eventLoadError(animeId: Int, sort: CommentSort, throwable: Throwable) {
        tracker.reportError(
            groupIdentifier = ERROR_LOAD,
            message = "$ERROR_LOAD_MESSAGE_PREFIX ($PARAM_ANIME_ID=$animeId, " +
                    "$PARAM_SORT=${sort.analyticsValue()}): ${throwable.analyticsType()}",
            throwable = throwable,
        )
    }

    /**
     * Ошибка загрузки ответов к комментарию.
     */
    fun eventRepliesLoadError(animeId: Int, commentId: Int, throwable: Throwable) {
        tracker.reportError(
            groupIdentifier = ERROR_REPLIES_LOAD,
            message = "$ERROR_REPLIES_LOAD_MESSAGE_PREFIX ($PARAM_ANIME_ID=$animeId, " +
                    "$PARAM_COMMENT_ID=$commentId): ${throwable.analyticsType()}",
            throwable = throwable,
        )
    }

    private fun eventWithAnime(eventName: String, animeId: Int, sort: CommentSort) {
        tracker.track(
            eventName,
            analyticsParamsOf(
                PARAM_ANIME_ID to animeId,
                PARAM_SORT to sort.analyticsValue(),
            ),
        )
    }

    private fun eventWithComment(eventName: String, animeId: Int, commentId: Int) {
        tracker.track(eventName, commentParams(animeId, commentId))
    }

    private fun commentParams(animeId: Int, commentId: Int): Map<String, String> =
        analyticsParamsOf(
            PARAM_ANIME_ID to animeId,
            PARAM_COMMENT_ID to commentId,
        )

    private fun CommentSort.analyticsValue(): String = name.lowercase()

    private fun CommentReportReason.analyticsValue(): String = name.lowercase()

    private fun CommentVote.analyticsValue(): String = name.lowercase()

    private fun Throwable.analyticsType(): String =
        this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

    internal companion object {
        private const val ERROR_LOAD = "comments_load_error"
        private const val ERROR_LOAD_MESSAGE_PREFIX = "Comments load failed"
        private const val ERROR_REPLIES_LOAD = "comments_replies_load_error"
        private const val ERROR_REPLIES_LOAD_MESSAGE_PREFIX = "Comment replies load failed"

        private const val PARAM_ANIME_ID = "anime_id"
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
