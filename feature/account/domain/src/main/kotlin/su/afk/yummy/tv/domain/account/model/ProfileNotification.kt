package su.afk.yummy.tv.domain.account.model

data class ProfileNotification(
    val id: Int,
    val dateSeconds: Long,
    val title: String,
    val text: String,
    val clickUri: String,
    val type: String,
    val subType: String,
    val viewed: Boolean,
    val objectId: Int?,
    val animeSlug: String? = null,
    val isNewEpisode: Boolean = false,
    val links: List<NotificationLink> = emptyList(),
    /** Название тайтла или поста, о котором уведомление; `null`, если сервер его не прислал. */
    val subjectTitle: String? = null,
    /** Id тайтла из уведомления; `null`, если сервер его не прислал. */
    val animeId: Int? = null,
)

/**
 * Цель уведомления о комментарии (`type = comment`) по его `clickUri`: `/posts/{id}#comment{n}`
 * или `/catalog/item/{slug}#comment{n}` (для тайтла нужен [animeId] из самого уведомления).
 * `null` — не комментарий или неизвестная цель.
 */
val ProfileNotification.commentTarget: NotificationCommentTarget?
    get() {
        if (!type.equals(COMMENT_NOTIFICATION_TYPE, ignoreCase = true)) return null
        val path = clickUri.substringBefore("#").substringBefore("?")
        path.substringAfter("/posts/", missingDelimiterValue = "")
            .substringBefore("/")
            .toIntOrNull()
            ?.let { return NotificationCommentTarget.Post(it) }
        return animeId?.let(NotificationCommentTarget::Anime)
    }

/**
 * К чему относится уведомление о комментарии: название тайтла или поста. `null` у остальных
 * типов (там название тайтла уже есть в заголовке) и если сервер его не прислал.
 */
val ProfileNotification.commentSubjectTitle: String?
    get() = subjectTitle?.takeIf { it.isNotBlank() && commentTarget != null }

/** У уведомления есть действие «Открыть» (новая серия или комментарий); доступно в раскрытом виде. */
val ProfileNotification.hasOpenAction: Boolean
    get() = (isNewEpisode && animeSlug != null) || commentTarget != null

private const val COMMENT_NOTIFICATION_TYPE = "comment"

/** Клик по уведомлению разворачивает его: полный текст и ссылки на аниме. */
val ProfileNotification.isExpandable: Boolean
    get() = text.isNotBlank() || links.isNotEmpty()

/**
 * Сколько непрочитанных в этой выборке ленты — число для бейджа аккаунта. Личные сообщения
 * исключены так же, как в [totalUnreadCount].
 */
fun List<ProfileNotification>.unreadBadgeCount(): Int =
    count { !it.viewed && !it.type.equals(MESSAGE_NOTIFICATION_TYPE, ignoreCase = true) }
