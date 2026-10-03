package su.afk.yummy.tv.data.pages.repository

import su.afk.yummy.tv.domain.pages.model.SitePageEmptyException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.utils.formatting.htmlToPlainText
import su.afk.yummy.tv.data.pages.mapper.findString
import su.afk.yummy.tv.data.pages.network.YaniPagesApi
import su.afk.yummy.tv.domain.pages.model.SitePage
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.domain.pages.repository.SitePagesRepository
import javax.inject.Inject

class YaniSitePagesRepository @Inject constructor(
    private val api: YaniPagesApi,
    private val cache: DocumentCacheStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : SitePagesRepository {
    override suspend fun getPage(type: SitePageType): SitePage {
        val language = settingsStore.currentLanguageCode()
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
        if (text.isBlank()) throw SitePageEmptyException()
        return SitePage(
            title = payload.findString(TITLE_KEYS)?.htmlToPlainText()?.trim().orEmpty(),
            text = text,
        )
    }

    companion object {
        const val SITE_PAGE_TTL_MS = 24 * 60 * 60 * 1000L
        val TITLE_KEYS = setOf("title", "name", "header")
        val CONTENT_KEYS = setOf("text_html", "html", "content", "text", "body")
    }
}
