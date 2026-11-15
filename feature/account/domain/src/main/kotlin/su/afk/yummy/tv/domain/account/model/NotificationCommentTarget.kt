package su.afk.yummy.tv.domain.account.model

/** Куда ведёт уведомление о комментарии: к чьим комментариям нужно перейти. */
sealed interface NotificationCommentTarget {
    /** Комментарии к посту блогера. */
    data class Post(val postId: Int) : NotificationCommentTarget

    /** Комментарии к тайтлу. */
    data class Anime(val animeId: Int) : NotificationCommentTarget
}
