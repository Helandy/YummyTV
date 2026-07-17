package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toAnimeListStateEntry
import su.afk.yummy.tv.data.account.storage.mapper.toUpdatedAnimeListStateEntry
import su.afk.yummy.tv.data.account.storage.mapper.toUserListCache
import su.afk.yummy.tv.data.account.storage.mapper.toUserListItems
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import su.afk.yummy.tv.data.account.storage.mapper.toUserListItem as toStoredUserListItem

class YaniUserListsRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorageStore,
    private val settingsStore: SettingsStore,
) : UserListsRepository {

    override suspend fun getUserList(
        userId: Int,
        list: UserAnimeList,
        forceRefresh: Boolean,
    ): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) {
            getUserList(userId, list.id, forceRefresh)
        }

    override suspend fun getUserFavorites(
        userId: Int,
        forceRefresh: Boolean,
    ): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) {
            getUserList(userId, FAVORITES_LIST_ID, forceRefresh)
        }

    override suspend fun hasCachedUserLists(userId: Int): Boolean =
        withContext(Dispatchers.IO) {
            accountStorage.hasUserListCache(userId)
        }

    override suspend fun getAnimeListState(animeId: Int): UserAnimeListItem? =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            if (userId <= 0) return@withContext null

            val stored = accountStorage.getAnimeListState(userId, animeId)
            if (stored?.isFresh(ANIME_LIST_STATE_TTL_MS) == true) {
                return@withContext stored.toStoredUserListItem()
            }

            try {
                fetchAnimeListState(userId, animeId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toStoredUserListItem()
                    ?: throw error
            }
        }

    override suspend fun setAnimeList(animeId: Int, list: UserAnimeList) =
        withContext(Dispatchers.IO) {
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

    override suspend fun setFavorite(animeId: Int, favorite: Boolean) =
        withContext(Dispatchers.IO) {
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

    private suspend fun getUserList(
        userId: Int,
        listId: Int,
        forceRefresh: Boolean,
    ): List<UserAnimeListItem> {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = accountStorage.getUserList(userId, listId, languageCode)
        if (!forceRefresh && stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
            return stored.toUserListItems()
        }

        return try {
            fetchUserList(userId, listId, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toUserListItems()
                ?: throw error
        }
    }

    private suspend fun fetchUserList(
        userId: Int,
        listId: Int,
        languageCode: String,
    ): List<UserAnimeListItem> {
        val cache = api.getUserList(userId, listId).toUserListCache(
            userId = userId,
            listId = listId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveUserList(cache)
        return cache.toUserListItems()
    }

    private suspend fun fetchAnimeListState(userId: Int, animeId: Int): UserAnimeListItem {
        val state = api.getAnimeListState(animeId)
        val entry = state.toAnimeListStateEntry(
            userId = userId,
            animeId = animeId,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveAnimeListState(entry)
        return entry.toStoredUserListItem()
    }

    private suspend fun updateCachedListState(
        userId: Int,
        animeId: Int,
        listId: Int? = null,
        updateList: Boolean = false,
        isFavorite: Boolean? = null,
    ) {
        val cached = accountStorage.getAnimeListState(userId, animeId)
        accountStorage.saveAnimeListState(
            cached.toUpdatedAnimeListStateEntry(
                userId = userId,
                animeId = animeId,
                listId = listId,
                updateList = updateList,
                isFavorite = isFavorite,
                cachedAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun invalidateUserLists(userId: Int) {
        if (userId > 0) {
            accountStorage.deleteUserLists(userId)
        }
    }

}
