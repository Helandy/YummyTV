package su.afk.yummy.tv.feature.comments.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.feature.comments.mobile.R

@Composable
internal fun CommentSort.label(): String = stringResource(
    when (this) {
        CommentSort.NEW -> R.string.comments_sort_new
        CommentSort.OLD -> R.string.comments_sort_old
        CommentSort.BEST -> R.string.comments_sort_best
    }
)

@Composable
internal fun CommentReportReason.label(): String = stringResource(
    when (this) {
        CommentReportReason.SPAM -> R.string.comments_report_spam
        CommentReportReason.INSULT -> R.string.comments_report_insult
        CommentReportReason.SPOILER -> R.string.comments_report_spoiler
        CommentReportReason.FLOOD -> R.string.comments_report_flood
        CommentReportReason.OFFTOPIC -> R.string.comments_report_offtopic
        CommentReportReason.OTHER -> R.string.comments_report_other
    }
)
