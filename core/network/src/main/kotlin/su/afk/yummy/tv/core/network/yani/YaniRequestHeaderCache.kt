package su.afk.yummy.tv.core.network.yani

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore

internal class YaniRequestHeaderCache(
    private val settingsStore: YaniAccountSettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val scope: CoroutineScope,
) {
    private val initialLoadMutex = Mutex()

    @Volatile
    private var loaded = false

    @Volatile
    private var applicationToken = ""

    @Volatile
    private var refreshToken = ""

    init {
        scope.launch {
            settingsStore.yaniApplicationToken.collectLatest { token ->
                applicationToken = token
            }
        }
        scope.launch {
            yaniAuthPreferences.refreshToken.collectLatest { token ->
                refreshToken = token
            }
        }
    }

    suspend fun current(): YaniRequestHeaders {
        if (!loaded) loadInitialValues()
        return YaniRequestHeaders(
            applicationToken = applicationToken,
            refreshToken = refreshToken,
            // Язык читаем из хранилища на каждый запрос: кэшированное поле отставало от смены языка,
            // и первый запрос после неё уходил со старым заголовком Lang.
            contentLanguageCode = settingsStore.yaniContentLanguage.first().apiCode,
        )
    }

    private suspend fun loadInitialValues() {
        initialLoadMutex.withLock {
            if (loaded) return
            coroutineScope {
                val applicationTokenDeferred = async { settingsStore.yaniApplicationToken.first() }
                val refreshTokenDeferred = async { yaniAuthPreferences.refreshToken.first() }
                applicationToken = applicationTokenDeferred.await()
                refreshToken = refreshTokenDeferred.await()
            }
            loaded = true
        }
    }
}

internal data class YaniRequestHeaders(
    val applicationToken: String,
    val refreshToken: String,
    val contentLanguageCode: String,
)
