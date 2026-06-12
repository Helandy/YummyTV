package su.afk.yummy.tv.data.top.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.withYaniContentLanguage
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.top.dto.YaniAnimeTopListDto
import su.afk.yummy.tv.data.top.mapper.toAnimeTopItem
import su.afk.yummy.tv.data.top.network.YaniAnimeTopApi
import su.afk.yummy.tv.domain.top.model.AnimeTopPage
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.repository.AnimeTopRepository

private const val ANIME_TOP_TTL_MS = 6 * 60 * 60 * 1000L

class YaniAnimeTopRepository(
    private val api: YaniAnimeTopApi,
    private val cache: CacheStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : AnimeTopRepository {

    override suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val response = cache.getOrFetch(
                key = "anime_top_${type.apiValue}_${limit}_$offset".withYaniContentLanguage(language),
                ttlMs = ANIME_TOP_TTL_MS,
                serialize = { dto: YaniAnimeTopListDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { api.getTopAnime(type, limit, offset) },
            ).response
            AnimeTopPage(
                items = response.mapNotNull { it.toAnimeTopItem() },
                nextOffset = offset + response.size,
                canLoadMore = response.size >= limit,
            )
        }
}
