package su.afk.yummy.tv.domain.comments.model

enum class CommentReportReason(val apiValue: Int) {
    SPAM(1),
    INSULT(2),
    SPOILER(3),
    FLOOD(4),
    OFFTOPIC(5),
    OTHER(8),
}
