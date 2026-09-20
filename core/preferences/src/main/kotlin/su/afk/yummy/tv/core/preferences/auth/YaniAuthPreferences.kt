package su.afk.yummy.tv.core.preferences.auth

import kotlinx.coroutines.flow.Flow

/**
 * Secure storage of the Yani refresh token — SharedPreferences + AndroidKeystore AES/GCM,
 * not DataStore, because the token must stay encrypted at rest. На прошивках с нерабочим
 * keymaster хранилище понижается до [TokenStorageMode.FALLBACK], иначе вход невозможен.
 */
interface YaniAuthPreferences {

    val refreshToken: Flow<String>

    /** Текущий режим хранения — нужен для диагностики на прошивках со сломанным Keystore. */
    val storageMode: Flow<TokenStorageMode>

    suspend fun setRefreshToken(token: String)

    suspend fun clearRefreshToken()
}
