package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.storage.account.AccountAnimeListStateEntry
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniAnimeListStateDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeListStateResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserListResponseDto
import su.afk.yummy.tv.data.account.mapper.toUserListCache
import su.afk.yummy.tv.data.account.mapper.toUserListItem
import su.afk.yummy.tv.data.account.mapper.toUserListItems
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository

class YaniUserListsRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val accountStorage: AccountStorageStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : UserListsRepository {

    override suspend fun getUserList(userId: Int, list: UserAnimeList): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) {
            getUserList(userId, list.id)
        }

    override suspend fun getUserFavorites(userId: Int): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) {
            getUserList(userId, FAVORITES_LIST_ID)
        }

    override suspend fun getAnimeListState(animeId: Int): UserAnimeListItem = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val stored = accountStorage.getAnimeListState(userId, animeId)
        if (stored?.isFresh(ANIME_LIST_STATE_TTL_MS) == true) {
            return@withContext stored.toUserListItem()
        }

        try {
            fetchAnimeListState(userId, animeId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toUserListItem()
                ?: readLegacyAnimeListState(userId, animeId)?.toUserListItem()
                ?: throw error
        }
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

    private suspend fun getUserList(userId: Int, listId: Int): List<UserAnimeListItem> {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = accountStorage.getUserList(userId, listId, languageCode)
        if (stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
            return stored.toUserListItems()
        }

        return try {
            fetchUserList(userId, listId, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toUserListItems()
                ?: readLegacyUserList(userId, listId, language, languageCode)
                ?: throw error
        }
    }

    private suspend fun fetchUserList(
        userId: Int,
        listId: Int,
        languageCode: String,
    ): List<UserAnimeListItem> {
        val items = api.getUserList(userId, listId).mapNotNull { it.toUserListItem() }
        accountStorage.saveUserList(
            items.toUserListCache(
                userId = userId,
                listId = listId,
                language = languageCode,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return items
    }

    private suspend fun fetchAnimeListState(userId: Int, animeId: Int): UserAnimeListItem {
        val state = api.getAnimeListState(animeId)
        val entry = state.toEntry(
            userId = userId,
            animeId = animeId,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveAnimeListState(entry)
        return entry.toUserListItem()
    }

    private suspend fun readLegacyUserList(
        userId: Int,
        listId: Int,
        language: YaniContentLanguage,
        languageCode: String,
    ): List<UserAnimeListItem>? {
        val cached = cache.getCached<YaniUserListResponseDto>(
            key = YaniAccountCacheKeys.userList(userId, listId, language),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val items = cached.value.response.mapNotNull { it.toUserListItem() }
        accountStorage.saveUserList(
            items.toUserListCache(
                userId = userId,
                listId = listId,
                language = languageCode,
                cachedAt = cached.cachedAt,
            )
        )
        return items
    }

    private suspend fun readLegacyAnimeListState(
        userId: Int,
        animeId: Int,
    ): AccountAnimeListStateEntry? {
        val cached = cache.getCached<YaniAnimeListStateResponseDto>(
            key = YaniAccountCacheKeys.animeListState(userId, animeId),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        return cached.value.response.toEntry(
            userId = userId,
            animeId = animeId,
            cachedAt = cached.cachedAt,
        ).also { accountStorage.saveAnimeListState(it) }
    }

    private suspend fun updateCachedListState(
        userId: Int,
        animeId: Int,
        listId: Int? = null,
        updateList: Boolean = false,
        isFavorite: Boolean? = null,
    ) {
        val cached = accountStorage.getAnimeListState(userId, animeId)
            ?: readLegacyAnimeListState(userId, animeId)
        accountStorage.saveAnimeListState(
            AccountAnimeListStateEntry(
                userId = userId,
                animeId = animeId,
                listId = if (updateList) listId else cached?.listId,
                isFavorite = isFavorite ?: (cached?.isFavorite == true),
                cachedAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun invalidateUserLists(userId: Int) {
        if (userId > 0) {
            accountStorage.deleteUserLists(userId)
            cache.invalidatePrefix(YaniAccountCacheKeys.userPrefix(userId) + "list_")
        }
    }

    private fun YaniAnimeListStateDto.toEntry(
        userId: Int,
        animeId: Int,
        cachedAt: Long,
    ): AccountAnimeListStateEntry =
        AccountAnimeListStateEntry(
            userId = userId,
            animeId = animeId,
            listId = list,
            isFavorite = isFavorite,
            cachedAt = cachedAt,
        )
}
