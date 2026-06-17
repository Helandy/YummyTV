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
import su.afk.yummy.tv.data.account.mapper.toAccount
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.network.YaniCaptchaRequiredException
import su.afk.yummy.tv.data.account.storage.mapper.toProfileEntry
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.data.account.storage.mapper.toAccount as toStoredAccount

class YaniAccountRepository(
    private val api: YaniAccountApi,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val accountStorage: AccountStorageStore,
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
                api.getProfile()
                    .toAccount()
                    .also { saveProfile(it) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toStoredAccount()
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
        return getStoredProfile(userId)?.toStoredAccount()
    }

    private suspend fun getStoredProfile(userId: Int) =
        accountStorage.getProfile(profileStorageKey(userId))
            ?: if (userId > 0) accountStorage.getProfile(ACCOUNT_PROFILE_KEY_CURRENT) else null

    private fun profileStorageKey(userId: Int): String =
        if (userId > 0) accountProfileUserKey(userId) else ACCOUNT_PROFILE_KEY_CURRENT
}
