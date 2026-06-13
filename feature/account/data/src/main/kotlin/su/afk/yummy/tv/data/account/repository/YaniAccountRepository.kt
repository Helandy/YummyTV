package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.ACCOUNT_PROFILE_KEY_CURRENT
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.accountProfileUserKey
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.mapper.toAccount
import su.afk.yummy.tv.data.account.mapper.toProfileEntry
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.network.YaniCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository

class YaniAccountRepository(
    private val api: YaniAccountApi,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val cache: CacheStore,
    private val accountStorage: AccountStorageStore,
    private val json: Json,
) : AccountRepository {

    override suspend fun login(
        login: String,
        password: String,
        captchaResponse: String?,
    ): YaniAccount = withContext(Dispatchers.IO) {
        val token = try {
            api.login(login, password, captchaResponse)
        } catch (e: YaniCaptchaRequiredException) {
            throw AccountCaptchaRequiredException()
        }
        if (token.isBlank()) error("Empty access token")
        val profileDto = api.getProfile(token)
        val profile = profileDto.toAccount()
        saveProfile(profile)
        settingsStore.setYaniAccount(profile.id, profile.nickname, profile.avatarUrl)
        yaniAuthPreferences.setRefreshToken(token)
        profile
    }

    override suspend fun refreshToken(): YaniAccount? = withContext(Dispatchers.IO) {
        val token = runCatching { api.refreshToken() }.getOrNull().orEmpty()
        if (token.isBlank()) return@withContext getCachedProfileOrNull()
        val profile = runCatching {
            val profileDto = api.getProfile(token)
            profileDto.toAccount().also { saveProfile(it) }
        }.getOrElse {
            getCachedProfileOrNull()
        }
        if (profile != null) {
            settingsStore.setYaniAccount(profile.id, profile.nickname, profile.avatarUrl)
            yaniAuthPreferences.setRefreshToken(token)
        }
        profile
    }

    override suspend fun getProfile(): YaniAccount =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.yaniUserId.first()
            val stored = getStoredProfile(userId)
            if (stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
                return@withContext stored.toAccount()
            }

            try {
                api.getProfile()
                    .toAccount()
                    .also { saveProfile(it) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toAccount()
                    ?: readLegacyProfile(userId)
                    ?: throw error
            }
        }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        val userId = settingsStore.yaniUserId.first()
        runCatching { api.logout() }
        if (userId > 0) {
            accountStorage.clearUserScoped(userId)
        } else {
            accountStorage.deleteProfile(ACCOUNT_PROFILE_KEY_CURRENT)
        }
        yaniAuthPreferences.clearRefreshToken()
        settingsStore.clearYaniAccount()
        cache.invalidatePrefix(YaniAccountCacheKeys.PRIVATE_USER_PREFIX)
        cache.invalidate(YaniAccountCacheKeys.profileCurrent())
    }

    private suspend fun saveProfile(
        profile: YaniAccount,
        cachedAt: Long = System.currentTimeMillis()
    ) {
        if (profile.id <= 0) return
        accountStorage.saveProfile(profile.toProfileEntry(ACCOUNT_PROFILE_KEY_CURRENT, cachedAt))
        accountStorage.saveProfile(
            profile.toProfileEntry(
                accountProfileUserKey(profile.id),
                cachedAt
            )
        )
    }

    private suspend fun getCachedProfileOrNull(): YaniAccount? {
        val userId = settingsStore.yaniUserId.first()
        getStoredProfile(userId)?.toAccount()?.let { return it }
        return readLegacyProfile(userId)
    }

    private suspend fun getStoredProfile(userId: Int) =
        accountStorage.getProfile(profileStorageKey(userId))
            ?: if (userId > 0) accountStorage.getProfile(ACCOUNT_PROFILE_KEY_CURRENT) else null

    private suspend fun readLegacyProfile(userId: Int): YaniAccount? {
        val keys = buildList {
            if (userId > 0) add(YaniAccountCacheKeys.profileUser(userId))
            add(YaniAccountCacheKeys.profileCurrent())
        }.distinct()

        for (key in keys) {
            val cached = cache.getCached<YaniProfileDto>(
                key = key,
                deserialize = { json.decodeFromString(it) },
                isValid = { it.id > 0 },
            ) ?: continue

            val profile = cached.value.toAccount()
            saveProfile(profile, cached.cachedAt)
            return profile
        }
        return null
    }

    private fun profileStorageKey(userId: Int): String =
        if (userId > 0) accountProfileUserKey(userId) else ACCOUNT_PROFILE_KEY_CURRENT
}
