package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.preferences.settings.currentUserId
import su.afk.yummy.tv.core.storage.account.AccountAnimeListStateEntry
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.AccountUserListCache
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toAnimeListStateEntry
import su.afk.yummy.tv.data.account.storage.mapper.toMergedUserListItems
import su.afk.yummy.tv.data.account.storage.mapper.toUpdatedAnimeListStateEntry
import su.afk.yummy.tv.data.account.storage.mapper.toUserListCache
import su.afk.yummy.tv.data.account.storage.mapper.toUserListItems
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import su.afk.yummy.tv.data.account.storage.mapper.toUserListItem as toStoredUserListItem

private const val FAVORITES_LIST_ID = 4
private val ALL_LIST_IDS = UserAnimeList.entries.map(UserAnimeList::id) + FAVORITES_LIST_ID

class YaniUserListsRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : UserListsRepository {

    override suspend fun getAllUserLists(
        userId: Int,
        forceRefresh: Boolean,
    ): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                forceRefresh = forceRefresh,
                // Только полный набор списков (все ALL_LIST_IDS) считается валидным кэшем —
                // частичный набор ведёт себя как "кэша нет" и во fresh-, и в fallback-ветке.
                read = {
                    ALL_LIST_IDS.mapNotNull { listId ->
                        accountStorage.getUserList(userId, listId, languageCode)
                    }.takeIf { it.size == ALL_LIST_IDS.size }
                },
                isFresh = { it.all { entry -> entry.isFresh(ACCOUNT_SHORT_TTL_MS) } },
                toDomain = { it.toMergedUserListItems(FAVORITES_LIST_ID) },
                fetchAndSave = { fetchAllUserLists(userId, languageCode) },
            )
        }

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
            val userId = settingsStore.currentUserId()
            if (userId <= 0) return@withContext null

            offlineFirstCache(
                read = { accountStorage.getAnimeListState(userId, animeId) },
                isFresh = { it.isFresh(ANIME_LIST_STATE_TTL_MS) },
                toDomain = { it.toStoredUserListItem() },
                fetchAndSave = { fetchAnimeListState(userId, animeId) },
            )
        }

    override suspend fun setAnimeList(animeId: Int, list: UserAnimeList) =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            api.setAnimeList(animeId, list.id)
            updateCachedListState(userId, animeId, listId = list.id, updateList = true)
            invalidateUserLists(userId)
            accountStorage.invalidateListStats(animeId)
        }

    override suspend fun removeAnimeList(animeId: Int) = withContext(Dispatchers.IO) {
        val userId = settingsStore.currentUserId()
        api.removeAnimeList(animeId)
        updateCachedListState(userId, animeId, listId = null, updateList = true)
        invalidateUserLists(userId)
        accountStorage.invalidateListStats(animeId)
    }

    override suspend fun setFavorite(animeId: Int, favorite: Boolean) =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            if (favorite) {
                api.setFavorite(animeId)
            } else {
                api.removeFavorite(animeId)
            }
            updateCachedListState(userId, animeId, isFavorite = favorite)
            invalidateUserLists(userId)
        }

    private suspend fun getUserList(
        userId: Int,
        listId: Int,
        forceRefresh: Boolean,
    ): List<UserAnimeListItem> {
        val languageCode = settingsStore.currentLanguageCode()
        return offlineFirstCache(
            forceRefresh = forceRefresh,
            read = { accountStorage.getUserList(userId, listId, languageCode) },
            isFresh = { it.isFresh(ACCOUNT_SHORT_TTL_MS) },
            toDomain = { it.toUserListItems() },
            fetchAndSave = { fetchUserList(userId, listId, languageCode) },
        )
    }

    private suspend fun fetchUserList(
        userId: Int,
        listId: Int,
        languageCode: String,
    ): AccountUserListCache {
        val cache = api.getUserList(userId, listId).toUserListCache(
            userId = userId,
            listId = listId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveUserList(cache)
        return cache
    }

    private suspend fun fetchAllUserLists(
        userId: Int,
        languageCode: String,
    ): List<AccountUserListCache> {
        val response = api.getAllUserLists(userId)
        val cachedAt = System.currentTimeMillis()
        val caches = ALL_LIST_IDS.map { listId ->
            response.filter { item ->
                if (listId == FAVORITES_LIST_ID) {
                    item.user?.list?.isFav == true
                } else {
                    item.user?.list?.list?.id == listId
                }
            }.toUserListCache(
                userId = userId,
                listId = listId,
                language = languageCode,
                cachedAt = cachedAt,
            )
        }
        accountStorage.saveUserLists(caches)
        return caches
    }

    private suspend fun fetchAnimeListState(userId: Int, animeId: Int): AccountAnimeListStateEntry {
        val state = api.getAnimeListState(animeId)
        val entry = state.toAnimeListStateEntry(
            userId = userId,
            animeId = animeId,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveAnimeListState(entry)
        return entry
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
