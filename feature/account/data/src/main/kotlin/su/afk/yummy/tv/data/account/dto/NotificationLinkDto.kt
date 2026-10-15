package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.Serializable

/** Ссылка на аниме из текста уведомления в том виде, как она лежит в кэше. */
@Serializable
internal data class NotificationLinkDto(
    val label: String,
    val slug: String,
)
