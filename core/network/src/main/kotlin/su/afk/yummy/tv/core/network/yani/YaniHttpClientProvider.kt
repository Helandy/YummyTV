package su.afk.yummy.tv.core.network.yani

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.api.YaniAuthPreferences
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YaniHttpClientProvider @Inject constructor(
    private val settingsStore: YaniAccountSettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val okHttpClient: OkHttpClient,
    @IoApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    @Volatile
    private var client: HttpClient? = null

    suspend fun get(): HttpClient {
        client?.let { return it }
        return mutex.withLock {
            client?.let { return it }
            withContext(Dispatchers.IO) {
                buildYaniHttpClient(settingsStore, yaniAuthPreferences, okHttpClient, scope)
            }.also { client = it }
        }
    }
}
