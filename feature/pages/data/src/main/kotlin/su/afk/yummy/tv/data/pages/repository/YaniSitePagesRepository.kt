package su.afk.yummy.tv.data.pages.repository

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.document.DocumentCacheStore
import su.afk.yummy.tv.core.utils.htmlToPlainText
import su.afk.yummy.tv.data.pages.network.YaniPagesApi
import su.afk.yummy.tv.domain.pages.model.SitePage
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.domain.pages.repository.SitePagesRepository
import javax.inject.Inject

class YaniSitePagesRepository @Inject constructor(
    private val api: YaniPagesApi,
    private val cache: DocumentCacheStore,
    private val settingsStore: SettingsStore,
) : SitePagesRepository {
    override suspend fun getPage(type: SitePageType): SitePage {
        val language = settingsStore.yaniContentLanguage.first().apiCode
        val body = cache.getOrFetch(
            cacheKey = "pages:$language:${type.apiValue}",
            ttlMs = SITE_PAGE_TTL_MS,
            decode = { it },
            encode = { it },
            fetch = { api.getPage(type.apiValue).also(::parsePage) },
        )
        return parsePage(body)
    }

    private fun parsePage(body: String): SitePage {
        val root = Json.parseToJsonElement(body)
        val payload = (root as? JsonObject)?.get("response") ?: root
        val rawText = (payload as? JsonPrimitive)?.contentOrNull
            ?: payload.findString(CONTENT_KEYS)
        val text = rawText?.htmlToPlainText()?.trim().orEmpty()
        require(text.isNotBlank()) { "Site page has no readable content" }
        return SitePage(
            title = payload.findString(TITLE_KEYS)?.htmlToPlainText()?.trim().orEmpty(),
            text = text,
        )
    }
}

private const val SITE_PAGE_TTL_MS = 24 * 60 * 60 * 1000L

private fun JsonElement.findString(keys: Set<String>): String? = when (this) {
    is JsonObject -> {
        entries.firstNotNullOfOrNull { (key, value) ->
            value.takeIf { key.lowercase() in keys }?.let { (it as? JsonPrimitive)?.contentOrNull }
        } ?: values.filterNot { it is JsonPrimitive }
            .firstNotNullOfOrNull { value -> value.findString(keys) }
    }

    is JsonArray -> firstNotNullOfOrNull { value -> value.findString(keys) }
    else -> null
}

private val TITLE_KEYS = setOf("title", "name", "header")
private val CONTENT_KEYS = setOf("text_html", "html", "content", "text", "body")
