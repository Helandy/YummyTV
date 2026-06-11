package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniAnimeListStateDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeListStateResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserListResponseDto
import su.afk.yummy.tv.data.account.mapper.toUserAnimeList
import su.afk.yummy.tv.data.account.mapper.toUserListItem
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository

class YaniUserListsRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : UserListsRepository {

    override suspend fun getUserList(userId: Int, list: UserAnimeList): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) {
            cache.getOrFetch(
                key = YaniAccountCacheKeys.userList(userId, list.id),
                ttlMs = ACCOUNT_SHORT_TTL_MS,
                serialize = { dto: YaniUserListResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { YaniUserListResponseDto(response = api.getUserList(userId, list.id)) },
            ).response.mapNotNull { it.toUserListItem() }
        }

    override suspend fun getUserFavorites(userId: Int): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) {
            cache.getOrFetch(
                key = YaniAccountCacheKeys.userList(userId, FAVORITES_LIST_ID),
                ttlMs = ACCOUNT_SHORT_TTL_MS,
                serialize = { dto: YaniUserListResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = {
                    YaniUserListResponseDto(
                        response = api.getUserList(
                            userId,
                            FAVORITES_LIST_ID
                        )
                    )
                },
            ).response.mapNotNull { it.toUserListItem() }
        }

    override suspend fun getAnimeListState(animeId: Int): UserAnimeListItem = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val state = cache.getOrFetch(
            key = YaniAccountCacheKeys.animeListState(userId, animeId),
            ttlMs = ANIME_LIST_STATE_TTL_MS,
            serialize = { dto: YaniAnimeListStateResponseDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { YaniAnimeListStateResponseDto(response = api.getAnimeListState(animeId)) },
        ).response
        UserAnimeListItem(
            animeId = animeId,
            title = "",
            posterUrl = null,
            rating = null,
            year = null,
            list = state.list.toUserAnimeList(),
            isFavorite = state.isFavorite,
        )
    }

    override suspend fun setAnimeList(animeId: Int, list: UserAnimeList) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        api.setAnimeList(animeId, list.id)
        updateCachedListState(userId, animeId, listId = list.id, updateList = true)
        invalidateUserLists(userId)
    }

    override suspend fun removeAnimeList(animeId: Int) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        api.removeAnimeList(animeId)
        updateCachedListState(userId, animeId, listId = null, updateList = true)
        invalidateUserLists(userId)
    }

    override suspend fun setFavorite(animeId: Int, favorite: Boolean) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        if (favorite) {
            api.setFavorite(animeId)
        } else {
            api.removeFavorite(animeId)
        }
        updateCachedListState(userId, animeId, isFavorite = favorite)
        invalidateUserLists(userId)
    }

    private companion object {
        const val FAVORITES_LIST_ID = 4
    }

    private suspend fun currentUserId(): Int =
        settingsStore.yaniUserId.first()

    private suspend fun getCachedListState(userId: Int, animeId: Int): YaniAnimeListStateDto? =
        runCatching {
            cache.getOrFetch(
                key = YaniAccountCacheKeys.animeListState(userId, animeId),
                ttlMs = 0L,
                serialize = { dto: YaniAnimeListStateResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { error("Anime list state refresh unavailable") },
            ).response
        }.getOrNull()

    private suspend fun updateCachedListState(
        userId: Int,
        animeId: Int,
        listId: Int? = null,
        updateList: Boolean = false,
        isFavorite: Boolean? = null,
    ) {
        val cached = getCachedListState(userId, animeId)
        cache.put(
            key = YaniAccountCacheKeys.animeListState(userId, animeId),
            serialize = { dto: YaniAnimeListStateResponseDto -> json.encodeToString(dto) },
            value = YaniAnimeListStateResponseDto(
                response = YaniAnimeListStateDto(
                    list = if (updateList) listId else cached?.list,
                    isFavorite = isFavorite ?: (cached?.isFavorite == true),
                )
            ),
        )
    }

    private suspend fun invalidateUserLists(userId: Int) {
        if (userId > 0) {
            cache.invalidatePrefix(YaniAccountCacheKeys.userPrefix(userId) + "list_")
        }
    }
}
