package su.afk.yummy.tv.data.account.utils

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.utils.formatting.htmlToPlainText
import su.afk.yummy.tv.data.account.dto.NotificationLinkDto
import su.afk.yummy.tv.domain.account.model.NotificationLink

private val ANCHOR_REGEX = Regex(
    "<a\\s[^>]*?href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>(.*?)</a>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val linksJson = Json { ignoreUnknownKeys = true }
private val linksSerializer = ListSerializer(NotificationLinkDto.serializer())

/** Slug аниме из каталожной ссылки `/catalog/item/{slug}`; для других ссылок — `null`. */
internal fun String.toCatalogItemSlug(): String? =
    substringAfter("/catalog/item/", missingDelimiterValue = "")
        .substringBefore("?")
        .substringBefore("#")
        .substringBefore("/")
        .takeIf { it.isNotBlank() }

/** Ссылки на аниме из HTML уведомления в порядке появления, без повторов по slug. */
internal fun String.toNotificationLinks(): List<NotificationLink> =
    ANCHOR_REGEX.findAll(this)
        .mapNotNull { match ->
            val slug = match.groupValues[1].toCatalogItemSlug() ?: return@mapNotNull null
            val label = match.groupValues[2].htmlToPlainText().ifBlank { return@mapNotNull null }
            NotificationLink(label = label, slug = slug)
        }
        .distinctBy { it.slug }
        .toList()

internal fun List<NotificationLink>.toLinksJson(): String =
    linksJson.encodeToString(linksSerializer, map { NotificationLinkDto(it.label, it.slug) })

internal fun String.fromLinksJson(): List<NotificationLink> =
    runCatching { linksJson.decodeFromString(linksSerializer, this) }
        .getOrDefault(emptyList())
        .map { NotificationLink(label = it.label, slug = it.slug) }
