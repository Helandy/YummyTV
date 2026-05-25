package su.afk.yummy.tv.data.account.repository

import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.data.account.mapper.toAccount
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.AccountRepository
import su.afk.yummy.tv.domain.account.YaniAccount

class YaniAccountRepository(
    private val api: YaniAccountApi,
    private val settingsStore: SettingsStore,
) : AccountRepository {

    override suspend fun login(login: String, password: String): YaniAccount {
        val token = api.login(login, password)
        if (token.isBlank()) error("Empty access token")
        settingsStore.setYaniAccount(accessToken = token, userId = 0, nickname = "", avatarUrl = null)
        val profile = getProfile()
        settingsStore.setYaniAccount(token, profile.id, profile.nickname, profile.avatarUrl)
        return profile
    }

    override suspend fun refreshToken(): YaniAccount? {
        val token = runCatching { api.refreshToken() }.getOrNull().orEmpty()
        if (token.isBlank()) return null
        val profile = runCatching { getProfile() }.getOrNull()
        settingsStore.setYaniAccount(token, profile?.id ?: 0, profile?.nickname.orEmpty(), profile?.avatarUrl)
        return profile
    }

    override suspend fun getProfile(): YaniAccount =
        api.getProfile().toAccount()

    override suspend fun logout() {
        runCatching { api.logout() }
        settingsStore.clearYaniAccount()
    }
}
