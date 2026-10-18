package su.afk.yummy.tv.feature.account.userprofile.model

import androidx.compose.runtime.Immutable

/** Комментарий пользователя под тайтлом в коротком виде для превью под статистикой. */
@Immutable
data class UserProfileCommentPreview(
    val id: Int,
    val text: String,
    val createdAtEpochSeconds: Long,
    val likes: Int,
    val dislikes: Int,
    /** Ник, которому отвечает комментарий (`[ник]…[/ник]` в начале текста). */
    val replyTo: String?,
    val animeId: Int?,
    val animeTitle: String?,
    val animePosterUrl: String?,
)
