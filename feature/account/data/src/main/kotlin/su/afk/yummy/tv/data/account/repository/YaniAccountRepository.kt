package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.ACCOUNT_PROFILE_KEY_CURRENT
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.accountProfileUserKey
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.document.DocumentCacheStore
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.dto.YaniRegistrationBodyDto
import su.afk.yummy.tv.data.account.mapper.toAccount
import su.afk.yummy.tv.data.account.mapper.toEditableProfile
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.network.YaniCaptchaRequiredException
import su.afk.yummy.tv.data.account.storage.mapper.toProfileEntry
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.EditableProfile
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.domain.account.model.UserRegistration
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.data.account.storage.mapper.toAccount as toStoredAccount

class YaniAccountRepository(
    private val api: YaniAccountApi,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val accountStorage: AccountStorageStore,
    private val documentCache: DocumentCacheStore,
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
        val savedProfile = saveProfile(profileDto)
        clearPreviousDocumentCacheIfNeeded(savedProfile.id)
        settingsStore.setYaniAccount(savedProfile.id, savedProfile.nickname, savedProfile.avatarUrl)
        yaniAuthPreferences.setRefreshToken(token)
        savedProfile
    }

    override suspend fun register(registration: UserRegistration) = withContext(Dispatchers.IO) {
        try {
            api.register(
                YaniRegistrationBodyDto(
                    email = registration.email,
                    password = registration.password,
                    username = registration.username,
                    captchaResponse = registration.captchaResponse,
                    hash = registration.hash,
                    shiki = registration.shikimori,
                    vk = registration.vk,
                )
            )
        } catch (_: YaniCaptchaRequiredException) {
            throw AccountCaptchaRequiredException()
        }
    }

    override suspend fun verifyRegistration(hash: String): YaniAccount =
        withContext(Dispatchers.IO) {
            val token = api.verifyRegistration(hash)
            val profileDto = api.getProfile(token)
            val savedProfile = saveProfile(profileDto)
            clearPreviousDocumentCacheIfNeeded(savedProfile.id)
            settingsStore.setYaniAccount(
                savedProfile.id,
                savedProfile.nickname,
                savedProfile.avatarUrl,
            )
            yaniAuthPreferences.setRefreshToken(token)
            savedProfile
        }

    override suspend fun refreshToken(): YaniAccount? = withContext(Dispatchers.IO) {
        val token = runCatching { api.refreshToken() }.getOrNull().orEmpty()
        if (token.isBlank()) return@withContext getCachedProfileOrNull()
        val profile = runCatching {
            val profileDto = api.getProfile(token)
            saveProfile(profileDto)
        }.getOrElse {
            getCachedProfileOrNull()
        }
        if (profile != null) {
            clearPreviousDocumentCacheIfNeeded(profile.id)
            settingsStore.setYaniAccount(profile.id, profile.nickname, profile.avatarUrl)
            yaniAuthPreferences.setRefreshToken(token)
        }
        profile
    }

    override fun observeSession() =
        combine(
            yaniAuthPreferences.refreshToken,
            settingsStore.yaniUserId,
        ) { token, userId ->
            AccountSession(
                isAuthorized = token.isNotBlank(),
                userId = userId,
            )
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override suspend fun getSession(): AccountSession =
        withContext(Dispatchers.IO) {
            AccountSession(
                isAuthorized = yaniAuthPreferences.refreshToken.first().isNotBlank(),
                userId = settingsStore.yaniUserId.first(),
            )
        }

    override suspend fun getProfile(): YaniAccount =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.yaniUserId.first()
            val stored = getStoredProfile(userId)
            if (stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
                return@withContext stored.toStoredAccount()
            }

            try {
                saveProfile(api.getProfile())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toStoredAccount()
                    ?: throw error
            }
        }

    override suspend fun refreshProfile(): EditableProfile = withContext(Dispatchers.IO) {
        val profileDto = api.getProfile()
        val savedProfile = saveProfile(profileDto)
        settingsStore.setYaniAccount(savedProfile.id, savedProfile.nickname, savedProfile.avatarUrl)
        if (savedProfile.id > 0) accountStorage.deleteUserProfileSummary(savedProfile.id)
        profileDto.toEditableProfile()
    }

    override suspend fun unlinkAccount(provider: LinkedAccountProvider): EditableProfile =
        withContext(Dispatchers.IO) {
            check(api.unlinkAccount(provider)) { "Account unlink was rejected" }
            val profileDto = api.getProfile()
            saveProfile(profileDto)
            profileDto.toEditableProfile()
        }

    override suspend fun updateOnlineStatus(deviceHash: String) = withContext(Dispatchers.IO) {
        api.updateOnline(deviceHash)
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        val userId = settingsStore.yaniUserId.first()
        runCatching { api.logout() }
        documentCache.deleteByPrefix(userDocumentCachePrefix(userId.coerceAtLeast(0)))
        if (userId > 0) {
            accountStorage.clearUserScoped(userId)
        } else {
            accountStorage.deleteProfile(ACCOUNT_PROFILE_KEY_CURRENT)
        }
        yaniAuthPreferences.clearRefreshToken()
        settingsStore.clearYaniAccount()
    }

    private suspend fun saveProfile(
        profile: YaniProfileDto,
        cachedAt: Long = System.currentTimeMillis()
    ): YaniAccount {
        if (profile.id <= 0) return profile.toAccount()
        val entry = profile.toProfileEntry(ACCOUNT_PROFILE_KEY_CURRENT, cachedAt)
        accountStorage.saveProfile(entry)
        accountStorage.saveProfile(
            profile.toProfileEntry(
                accountProfileUserKey(profile.id),
                cachedAt
            )
        )
        // Возвращаем через тот же cache->domain маппер, что и при чтении из кэша, чтобы
        // свежая загрузка не расходилась с последующим чтением.
        return entry.toStoredAccount()
    }

    private suspend fun getCachedProfileOrNull(): YaniAccount? {
        val userId = settingsStore.yaniUserId.first()
        return getStoredProfile(userId)?.toStoredAccount()
    }

    private suspend fun getStoredProfile(userId: Int) =
        accountStorage.getProfile(profileStorageKey(userId))
            ?: if (userId > 0) accountStorage.getProfile(ACCOUNT_PROFILE_KEY_CURRENT) else null

    private fun profileStorageKey(userId: Int): String =
        if (userId > 0) accountProfileUserKey(userId) else ACCOUNT_PROFILE_KEY_CURRENT

    private suspend fun clearPreviousDocumentCacheIfNeeded(newUserId: Int) {
        val previousUserId = settingsStore.yaniUserId.first()
        if (previousUserId != newUserId) {
            documentCache.deleteByPrefix(userDocumentCachePrefix(previousUserId.coerceAtLeast(0)))
            if (previousUserId > 0) accountStorage.clearUserScoped(previousUserId)
        }
    }

    private fun userDocumentCachePrefix(userId: Int): String = "user:$userId:"
}
