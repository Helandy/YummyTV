package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toCollectionSummaries
import su.afk.yummy.tv.data.account.storage.mapper.toCollectionsPageCache
import su.afk.yummy.tv.data.account.storage.mapper.toUserFriends
import su.afk.yummy.tv.data.account.storage.mapper.toUserFriendsPageCache
import su.afk.yummy.tv.data.account.storage.mapper.toUserPosts
import su.afk.yummy.tv.data.account.storage.mapper.toUserPostsPageCache
import su.afk.yummy.tv.data.account.storage.mapper.toUserReviews
import su.afk.yummy.tv.data.account.storage.mapper.toUserReviewsPageCache
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.domain.account.repository.UserProfileContentRepository

class YaniUserProfileContentRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : UserProfileContentRepository {
    override suspend fun getFriends(userId: Int, limit: Int, offset: Int): List<UserFriend> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                read = { accountStorage.getUserFriends(userId, languageCode, limit, offset) },
                isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
                toDomain = { it.toUserFriends() },
                fetchAndSave = {
                    val cache = api.getUserFriends(userId, limit, offset).toUserFriendsPageCache(
                        userId = userId,
                        language = languageCode,
                        limit = limit,
                        offset = offset,
                        cachedAt = System.currentTimeMillis(),
                    )
                    accountStorage.saveUserFriends(cache)
                    cache
                },
            )
        }

    override suspend fun getReviews(userId: Int, limit: Int, offset: Int): List<UserReviewSummary> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                read = { accountStorage.getUserReviews(userId, languageCode, limit, offset) },
                isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
                toDomain = { it.toUserReviews() },
                fetchAndSave = {
                    val cache = api.getUserReviews(userId, limit, offset).toUserReviewsPageCache(
                        userId = userId,
                        language = languageCode,
                        limit = limit,
                        offset = offset,
                        cachedAt = System.currentTimeMillis(),
                    )
                    accountStorage.saveUserReviews(cache)
                    cache
                },
            )
        }

    override suspend fun getPosts(userId: Int, limit: Int, offset: Int): List<UserPostSummary> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                read = { accountStorage.getUserPosts(userId, languageCode, limit, offset) },
                isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
                toDomain = { it.toUserPosts() },
                fetchAndSave = {
                    val cache = api.getUserPosts(userId, limit, offset).toUserPostsPageCache(
                        userId = userId,
                        language = languageCode,
                        limit = limit,
                        offset = offset,
                        cachedAt = System.currentTimeMillis(),
                    )
                    accountStorage.saveUserPosts(cache)
                    cache
                },
            )
        }

    override suspend fun getCollections(
        userId: Int,
        limit: Int,
        offset: Int,
    ): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            val pageKey = userCollectionsPageKey(userId, limit, offset, languageCode)
            offlineFirstCache(
                read = { accountStorage.getCollections(pageKey) },
                isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
                toDomain = { it.toCollectionSummaries() },
                fetchAndSave = {
                    val cachedAt = System.currentTimeMillis()
                    val cache =
                        api.getUserCollections(userId, limit, offset).toCollectionsPageCache(
                            pageKey = pageKey,
                            language = languageCode,
                            cachedAt = cachedAt,
                        )
                    accountStorage.saveCollections(
                        cache,
                        prunePagesCachedBefore = cachedAt - ACCOUNT_PAGE_CACHE_RETENTION_MS,
                    )
                    cache
                },
            )
        }

    private fun userCollectionsPageKey(
        userId: Int,
        limit: Int,
        offset: Int,
        language: String,
    ): String =
        "user:$userId:collections:$limit:$offset:$language"
}
