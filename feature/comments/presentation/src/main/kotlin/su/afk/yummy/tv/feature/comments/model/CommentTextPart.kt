package su.afk.yummy.tv.feature.comments.model

/** Фрагмент текста комментария: обычный текст или спойлер, скрытый под заголовком. */
sealed interface CommentTextPart {
    data class Plain(val text: String) : CommentTextPart
    data class Spoiler(val title: String, val text: String) : CommentTextPart
}
