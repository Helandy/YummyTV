package su.afk.yummy.tv.core.preferences.auth

import kotlinx.coroutines.flow.Flow

/** Secure storage of the Yani refresh token. */
interface YaniAuthPreferences {

    val refreshToken: Flow<String>

    suspend fun setRefreshToken(token: String)

    suspend fun clearRefreshToken()
}
