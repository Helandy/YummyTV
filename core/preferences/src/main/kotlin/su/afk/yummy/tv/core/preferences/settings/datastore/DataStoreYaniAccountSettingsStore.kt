package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.YaniApplicationTokenState
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.hiddenRecommendationIdsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniAccessTokenKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniApplicationTokenKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniAvatarUrlKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniContentLanguageKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniNicknameKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniTokenRefreshAtKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniUnreadNotificationsCountKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniUserIdKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreYaniAccountSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : YaniAccountSettingsStore {

    override val yaniApplicationToken: Flow<String> =
        store.data.map { prefs -> prefs.yaniApplicationToken() }

    override val yaniApplicationTokenState: Flow<YaniApplicationTokenState> =
        store.data.map { prefs -> prefs.yaniApplicationTokenState() }

    override val yaniUserId: Flow<Int> = store.int(yaniUserIdKey)

    override val yaniNickname: Flow<String> = store.string(yaniNicknameKey)

    override val yaniAvatarUrl: Flow<String> = store.string(yaniAvatarUrlKey)

    override val yaniTokenRefreshAt: Flow<Long> = store.data.map { prefs ->
        prefs[yaniTokenRefreshAtKey]?.toLongOrNull() ?: 0L
    }

    override val yaniUnreadNotificationsCount: Flow<Int> =
        store.int(yaniUnreadNotificationsCountKey)

    override val yaniContentLanguage: Flow<YaniContentLanguage> = store.data.map { prefs ->
        YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey])
            ?: store.resolveSystemContentLanguage()
    }

    override val hiddenRecommendationIds: Flow<Set<Int>> = store.data.map { prefs ->
        prefs[hiddenRecommendationIdsKey].orEmpty().mapNotNull(String::toIntOrNull).toSet()
    }

    override suspend fun setYaniApplicationToken(token: String) {
        store.edit { prefs ->
            val trimmedToken = token.trim()
            if (trimmedToken.isBlank()) {
                prefs.remove(yaniApplicationTokenKey)
            } else {
                prefs[yaniApplicationTokenKey] = trimmedToken
            }
        }
    }

    override suspend fun setYaniAccount(
        userId: Int,
        nickname: String,
        avatarUrl: String?,
        refreshedAt: Long,
    ) {
        store.edit { prefs ->
            prefs[yaniUserIdKey] = userId
            prefs[yaniNicknameKey] = nickname
            prefs[yaniTokenRefreshAtKey] = refreshedAt.toString()
            if (avatarUrl.isNullOrBlank()) {
                prefs.remove(yaniAvatarUrlKey)
            } else {
                prefs[yaniAvatarUrlKey] = avatarUrl
            }
        }
    }

    override suspend fun clearLegacyYaniAccessToken() {
        store.edit { prefs -> prefs.remove(yaniAccessTokenKey) }
    }

    override suspend fun setRecommendationHidden(animeId: Int, hidden: Boolean) {
        store.edit { prefs ->
            val current = prefs[hiddenRecommendationIdsKey].orEmpty()
            val id = animeId.toString()
            prefs[hiddenRecommendationIdsKey] = if (hidden) current + id else current - id
        }
    }

    override suspend fun clearYaniAccount() {
        store.edit { prefs ->
            prefs.remove(yaniAccessTokenKey)
            prefs.remove(yaniUserIdKey)
            prefs.remove(yaniNicknameKey)
            prefs.remove(yaniAvatarUrlKey)
            prefs.remove(yaniTokenRefreshAtKey)
            prefs.remove(yaniUnreadNotificationsCountKey)
            // Скрытые рекомендации привязаны к аккаунту — новому пользователю они не нужны.
            prefs.remove(hiddenRecommendationIdsKey)
        }
    }

    override suspend fun setYaniUnreadNotificationsCount(count: Int) {
        store.edit { prefs ->
            prefs[yaniUnreadNotificationsCountKey] = count.coerceAtLeast(0)
        }
    }

    override suspend fun setYaniContentLanguage(language: YaniContentLanguage) =
        store.setEnum(yaniContentLanguageKey, language)

    override suspend fun ensureYaniContentLanguageInitialized() {
        store.edit { prefs ->
            if (YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey]) == null) {
                prefs[yaniContentLanguageKey] = store.resolveSystemContentLanguage().name
            }
        }
    }
}
