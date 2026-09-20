package su.afk.yummy.tv.core.preferences.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import javax.inject.Inject

/** При первом чтении once чистит легаси access-token, мигрировавший из [YaniAccountSettingsStore]. */
internal class SecureYaniAuthPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsStore: YaniAccountSettingsStore,
    private val analyticsTracker: AnalyticsTracker,
) : YaniAuthPreferences {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val migrationMutex = Mutex()
    private val storageMutex = Mutex()
    private val storageModeState = MutableStateFlow(readPersistedMode())

    private val storage: AuthTokenStorage by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AuthTokenStorage(
            store = prefsRecordStore(),
            cipherFactory = { mode ->
                when (mode) {
                    TokenStorageMode.KEYSTORE -> KeystoreTokenCipher()
                    TokenStorageMode.FALLBACK -> FallbackTokenCipher()
                }
            },
            onStorageFailure = ::reportStorageFailure,
            onModeChanged = { mode -> storageModeState.value = mode },
        )
    }

    override val storageMode: Flow<TokenStorageMode> = storageModeState.asStateFlow()

    override val refreshToken: Flow<String> = callbackFlow {
        clearLegacyTokenIfNeeded()
        trySend(readToken())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_REFRESH_TOKEN) launch { trySend(readToken()) }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    override suspend fun setRefreshToken(token: String) {
        withContext(Dispatchers.IO) {
            storageMutex.withLock { storage.write(token) }
        }
    }

    override suspend fun clearRefreshToken() {
        withContext(Dispatchers.IO) {
            storageMutex.withLock { storage.clear() }
        }
    }

    private suspend fun readToken(): String = storageMutex.withLock { storage.read() }

    private suspend fun clearLegacyTokenIfNeeded() {
        migrationMutex.withLock {
            settingsStore.clearLegacyYaniAccessToken()
        }
    }

    private fun prefsRecordStore() = object : TokenRecordStore {
        override fun readRecord(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

        override fun writeRecord(record: String) {
            prefs.edit().putString(KEY_REFRESH_TOKEN, record).apply()
        }

        override fun removeRecord() {
            prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
        }

        override fun readMode(): TokenStorageMode = readPersistedMode()

        override fun writeMode(mode: TokenStorageMode) {
            prefs.edit().putString(KEY_STORAGE_MODE, mode.name).apply()
        }
    }

    private fun readPersistedMode(): TokenStorageMode =
        prefs.getString(KEY_STORAGE_MODE, null)
            ?.let { name -> TokenStorageMode.entries.firstOrNull { it.name == name } }
            ?: TokenStorageMode.KEYSTORE

    /** Прошивка важнее стектрейса: по ней видно, что отказ keystore — не единичный случай. */
    private fun reportStorageFailure(message: String, error: Throwable?) {
        val details = buildString {
            append(message)
            append(" | ").append(Build.MANUFACTURER).append('/').append(Build.MODEL)
            append('/').append(Build.DEVICE)
            append(" sdk=").append(Build.VERSION.SDK_INT)
            append(" fingerprint=").append(Build.FINGERPRINT)
        }
        // Репорт не должен ронять поток токена: исключение отсюда прилетело бы в
        // SupervisorJob-скоуп YaniRequestHeaderCache и убило бы процесс.
        runCatching {
            analyticsTracker.reportError(
                details,
                error ?: IllegalStateException(message),
                STORAGE_GROUP,
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "yani_auth_secure_preferences"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_STORAGE_MODE = "token_storage_mode"
        private const val STORAGE_GROUP = "auth_token_storage"
    }
}
