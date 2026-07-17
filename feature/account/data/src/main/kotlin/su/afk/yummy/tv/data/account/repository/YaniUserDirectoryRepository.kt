package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.data.account.mapper.toFriendshipStatus
import su.afk.yummy.tv.data.account.mapper.toUserSearchItem
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toUserProfileSummary
import su.afk.yummy.tv.data.account.storage.mapper.toUserProfileSummaryCache
import su.afk.yummy.tv.domain.account.model.FriendshipStatus
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserSearchItem
import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository

class YaniUserDirectoryRepository(
    private val api: YaniAccountApi,
    private val storage: AccountStorageStore,
    private val settingsStore: SettingsStore,
) : UserDirectoryRepository {
    override suspend fun search(query: String, limit: Int, offset: Int): List<UserSearchItem> =
        withContext(Dispatchers.IO) {
            api.searchUsers(query, limit, offset)
                .filter { it.id > 0 && it.nickname.isNotBlank() }
                .map { it.toUserSearchItem() }
        }

    override suspend fun getProfileByNickname(nickname: String): UserProfileSummary =
        withContext(Dispatchers.IO) {
            val profile = api.getUserProfileByNickname(nickname).response
            val language = settingsStore.yaniContentLanguage.first().apiCode
            val cache = profile.toUserProfileSummaryCache(
                userId = profile.id,
                language = language,
                cachedAt = System.currentTimeMillis(),
            )
            storage.saveUserProfileSummary(cache)
            cache.toUserProfileSummary()
        }

    override suspend fun getFriendship(userId: Int, friendId: Int): FriendshipStatus =
        withContext(Dispatchers.IO) {
            api.getFriendshipStatus(userId, friendId).toFriendshipStatus()
        }

    override suspend fun addFriend(userId: Int, friendId: Int) = withContext(Dispatchers.IO) {
        api.addFriend(userId, friendId)
        invalidateFriendCaches(userId, friendId)
    }

    override suspend fun removeFriend(userId: Int, friendId: Int) = withContext(Dispatchers.IO) {
        api.removeFriend(userId, friendId)
        invalidateFriendCaches(userId, friendId)
    }

    private suspend fun invalidateFriendCaches(userId: Int, friendId: Int) {
        storage.deleteUserFriends(userId)
        storage.deleteUserFriends(friendId)
        storage.deleteUserProfileSummary(userId)
        storage.deleteUserProfileSummary(friendId)
    }
}
