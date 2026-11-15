package su.afk.yummy.tv.data.account.utils

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** О чём уведомление: название тайтла или поста и, для тайтла, его id. */
internal data class NotificationSubject(
    val title: String,
    val animeId: Int?,
)

/**
 * Тайтл или пост из поля `data` уведомления (`data.anime` / `data.post`). `data` у разных типов
 * устроено по-разному, поэтому разбираем вручную и при любом несовпадении формы возвращаем `null`.
 */
internal fun JsonElement?.toNotificationSubject(): NotificationSubject? {
    val data = this as? JsonObject ?: return null
    val anime = data["anime"] as? JsonObject
    val post = data["post"] as? JsonObject
    val source = anime ?: post ?: return null
    val title = source.stringOrNull("title")?.takeIf { it.isNotBlank() } ?: return null
    return NotificationSubject(
        title = title,
        animeId = anime?.get("anime_id")?.runCatching { jsonPrimitive.intOrNull }?.getOrNull(),
    )
}

private fun JsonObject.stringOrNull(key: String): String? =
    get(key)?.runCatching { jsonPrimitive.contentOrNull }?.getOrNull()
