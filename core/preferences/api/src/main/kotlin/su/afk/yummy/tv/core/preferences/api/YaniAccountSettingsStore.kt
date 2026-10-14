package su.afk.yummy.tv.core.preferences.api

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.settings.YaniApplicationTokenState
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage

/** Идентичность и учётная запись Yani: токен, профиль, язык контента. */
interface YaniAccountSettingsStore {

    val yaniApplicationToken: Flow<String>
    val yaniApplicationTokenState: Flow<YaniApplicationTokenState>
    val yaniUserId: Flow<Int>
    val yaniNickname: Flow<String>
    val yaniAvatarUrl: Flow<String>
    val yaniTokenRefreshAt: Flow<Long>
    val yaniUnreadNotificationsCount: Flow<Int>
    val yaniContentLanguage: Flow<YaniContentLanguage>

    /** Тайтлы, которые пользователь попросил больше не рекомендовать. */
    val hiddenRecommendationIds: Flow<Set<Int>>

    suspend fun setYaniApplicationToken(token: String)

    suspend fun setYaniAccount(
        userId: Int,
        nickname: String,
        avatarUrl: String?,
        refreshedAt: Long = System.currentTimeMillis(),
    )

    suspend fun clearLegacyYaniAccessToken()
    suspend fun setRecommendationHidden(animeId: Int, hidden: Boolean)
    suspend fun clearYaniAccount()
    suspend fun setYaniUnreadNotificationsCount(count: Int)
    suspend fun setYaniContentLanguage(language: YaniContentLanguage)
    suspend fun ensureYaniContentLanguageInitialized()
}
