package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.mapper.toAccount
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
        cacheProfile(profileDto)
        val profile = profileDto.toAccount()
        settingsStore.setYaniAccount(profile.id, profile.nickname, profile.avatarUrl)
        yaniAuthPreferences.setRefreshToken(token)
        profile
    }

    override suspend fun refreshToken(): YaniAccount? = withContext(Dispatchers.IO) {
        val token = runCatching { api.refreshToken() }.getOrNull().orEmpty()
        if (token.isBlank()) return@withContext getCachedProfileOrNull()?.toAccount()
        val profile = runCatching {
            val profileDto = api.getProfile(token)
            cacheProfile(profileDto)
            profileDto.toAccount()
        }.getOrElse {
            getCachedProfileOrNull()?.toAccount()
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
            val key = if (userId > 0) {
                YaniAccountCacheKeys.profileUser(userId)
            } else {
                YaniAccountCacheKeys.profileCurrent()
            }
            cache.getOrFetch(
                key = key,
                ttlMs = ACCOUNT_SHORT_TTL_MS,
                serialize = { dto: YaniProfileDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { api.getProfile() },
                isValid = { it.id > 0 },
            ).also { cacheProfile(it) }.toAccount()
        }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { api.logout() }
        yaniAuthPreferences.clearRefreshToken()
        settingsStore.clearYaniAccount()
        cache.invalidatePrefix(YaniAccountCacheKeys.PRIVATE_USER_PREFIX)
        cache.invalidate(YaniAccountCacheKeys.profileCurrent())
    }

    private suspend fun cacheProfile(profile: YaniProfileDto) {
        if (profile.id <= 0) return
        cache.put(
            key = YaniAccountCacheKeys.profileCurrent(),
            serialize = { dto: YaniProfileDto -> json.encodeToString(dto) },
            value = profile,
        )
        cache.put(
            key = YaniAccountCacheKeys.profileUser(profile.id),
            serialize = { dto: YaniProfileDto -> json.encodeToString(dto) },
            value = profile,
        )
    }

    private suspend fun getCachedProfileOrNull(): YaniProfileDto? {
        val userId = settingsStore.yaniUserId.first()
        val key = if (userId > 0) {
            YaniAccountCacheKeys.profileUser(userId)
        } else {
            YaniAccountCacheKeys.profileCurrent()
        }
        return runCatching {
            cache.getOrFetch(
                key = key,
                ttlMs = 0L,
                serialize = { dto: YaniProfileDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { error("Profile refresh unavailable") },
                isValid = { it.id > 0 },
            )
        }.getOrNull()
    }
}
