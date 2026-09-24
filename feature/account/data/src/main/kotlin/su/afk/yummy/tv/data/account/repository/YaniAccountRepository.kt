package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.ACCOUNT_PROFILE_KEY_CURRENT
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.accountProfileUserKey
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.dto.YaniRegistrationBodyDto
import su.afk.yummy.tv.data.account.mapper.toAccount
import su.afk.yummy.tv.data.account.mapper.toEditableProfile
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.network.YaniAccountException
import su.afk.yummy.tv.data.account.network.YaniCaptchaRequiredException
import su.afk.yummy.tv.data.account.storage.mapper.toProfileEntry
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.EditableProfile
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.domain.account.model.LoginException
import su.afk.yummy.tv.domain.account.model.RegistrationException
import su.afk.yummy.tv.domain.account.model.UserRegistration
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.data.account.storage.mapper.toAccount as toStoredAccount

class YaniAccountRepository(
    private val api: YaniAccountApi,
    private val settingsStore: YaniAccountSettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val accountStorage: AccountStorage,
    private val documentCache: DocumentCacheStorage,
    private val animeStorage: AnimeStorage,
    private val analyticsTracker: AnalyticsTracker,
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
        } catch (e: YaniAccountException) {
            throw LoginException(e.message ?: "Could not sign in")
        }
        signInWithTokenInternal(token)
    }

    override suspend fun signInWithToken(token: String): YaniAccount = withContext(Dispatchers.IO) {
        signInWithTokenInternal(token)
    }

    /**
     * Вход атомарен. Токен — единственный носитель [AccountSession.isAuthorized], поэтому он
     * пишется первым: иначе отказ на записи профиля или настроек оставлял приложение
     * полувошедшим (запросы уже авторизованы, а экран входа крутится по кругу). Любая ошибка
     * после записи токена откатывает сессию целиком.
     */
    private suspend fun signInWithTokenInternal(token: String): YaniAccount {
        if (token.isBlank()) error("Empty access token")
        val profileDto = api.getProfile(token)
        yaniAuthPreferences.setRefreshToken(token)
        return try {
            val savedProfile = saveProfileAfterSignIn(profileDto)
            // Порядок важен: чистка сравнивает прошлый userId, который затрёт setYaniAccount.
            clearPreviousDocumentCacheAfterSignIn(savedProfile.id)
            settingsStore.setYaniAccount(
                savedProfile.id,
                savedProfile.nickname,
                savedProfile.avatarUrl,
            )
            savedProfile
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            rollbackSignIn()
            throw error
        }
    }

    /** Кэш профиля не критичен для входа — он всё равно перечитается через [getProfile]. */
    private suspend fun saveProfileAfterSignIn(profile: YaniProfileDto): YaniAccount =
        runSuspendCatching {
            saveProfile(profile)
        }.getOrElse { error ->
            analyticsTracker.reportError("Sign-in profile cache write failed", error, SIGN_IN_GROUP)
            profile.toAccount()
        }

    private suspend fun clearPreviousDocumentCacheAfterSignIn(newUserId: Int) {
        runSuspendCatching {
            clearPreviousDocumentCacheIfNeeded(newUserId)
        }.getOrElse { error ->
            analyticsTracker.reportError("Sign-in cache cleanup failed", error, SIGN_IN_GROUP)
        }
    }

    private suspend fun rollbackSignIn() {
        runSuspendCatching { yaniAuthPreferences.clearRefreshToken() }
        runSuspendCatching { settingsStore.clearYaniAccount() }
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
                ),
            )
        } catch (_: YaniCaptchaRequiredException) {
            throw AccountCaptchaRequiredException()
        } catch (e: YaniAccountException) {
            throw RegistrationException(e.message ?: "Could not register user")
        }
    }

    override suspend fun verifyRegistration(hash: String): YaniAccount =
        withContext(Dispatchers.IO) {
            signInWithTokenInternal(api.verifyRegistration(hash))
        }

    override suspend fun refreshToken(): YaniAccount? = withContext(Dispatchers.IO) {
        val token = runSuspendCatching { api.refreshToken() }.getOrNull().orEmpty()
        if (token.isBlank()) return@withContext getCachedProfileOrNull()
        val profile = runSuspendCatching {
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

            runSuspendCatching {
                saveProfile(api.getProfile())
            }.getOrElse { error ->
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
        runSuspendCatching { api.logout() }
        documentCache.deleteByPrefix(userDocumentCachePrefix(userId.coerceAtLeast(0)))
        if (userId > 0) {
            accountStorage.clearUserScoped(userId)
        } else {
            accountStorage.deleteProfile(ACCOUNT_PROFILE_KEY_CURRENT)
        }
        // В кэше видео лежат привязанные к пользователю watched и subscribed — они больше не наши.
        animeStorage.expireAllVideos()
        yaniAuthPreferences.clearRefreshToken()
        settingsStore.clearYaniAccount()
    }

    private suspend fun saveProfile(
        profile: YaniProfileDto,
        cachedAt: Long = System.currentTimeMillis(),
    ): YaniAccount {
        if (profile.id <= 0) return profile.toAccount()
        val entry = profile.toProfileEntry(ACCOUNT_PROFILE_KEY_CURRENT, cachedAt)
        accountStorage.saveProfile(entry)
        accountStorage.saveProfile(
            profile.toProfileEntry(
                accountProfileUserKey(profile.id),
                cachedAt,
            ),
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

    private companion object {
        const val SIGN_IN_GROUP = "sign_in"
    }
}
