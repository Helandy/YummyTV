package su.afk.yummy.tv.feature.comments.tv.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.feature.comments.tv.R

@StringRes
internal fun CommentSort.labelRes(): Int = when (this) {
    CommentSort.NEW -> R.string.comments_sort_new
    CommentSort.OLD -> R.string.comments_sort_old
    CommentSort.BEST -> R.string.comments_sort_best
}

@StringRes
internal fun CommentReportReason.labelRes(): Int = when (this) {
    CommentReportReason.SPAM -> R.string.comments_report_spam
    CommentReportReason.INSULT -> R.string.comments_report_insult
    CommentReportReason.SPOILER -> R.string.comments_report_spoiler
    CommentReportReason.FLOOD -> R.string.comments_report_flood
    CommentReportReason.OFFTOPIC -> R.string.comments_report_offtopic
    CommentReportReason.OTHER -> R.string.comments_report_other
}
