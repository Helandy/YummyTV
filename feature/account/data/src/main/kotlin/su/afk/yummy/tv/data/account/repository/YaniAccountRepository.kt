package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.data.account.mapper.toAccount
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.network.YaniCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository

class YaniAccountRepository(
    private val api: YaniAccountApi,
    private val settingsStore: SettingsStore,
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
        settingsStore.setYaniAccount(accessToken = token, userId = 0, nickname = "", avatarUrl = null)
        val profile = getProfile()
        settingsStore.setYaniAccount(token, profile.id, profile.nickname, profile.avatarUrl)
        profile
    }

    override suspend fun refreshToken(): YaniAccount? = withContext(Dispatchers.IO) {
        val token = runCatching { api.refreshToken() }.getOrNull().orEmpty()
        if (token.isBlank()) return@withContext null
        val profile = runCatching { getProfile() }.getOrNull()
        settingsStore.setYaniAccount(token, profile?.id ?: 0, profile?.nickname.orEmpty(), profile?.avatarUrl)
        profile
    }

    override suspend fun getProfile(): YaniAccount =
        withContext(Dispatchers.IO) { api.getProfile().toAccount() }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { api.logout() }
        settingsStore.clearYaniAccount()
    }
}
