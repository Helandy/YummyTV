package su.afk.yummy.tv.data.account.backup

import android.content.Context
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData
import kotlinx.coroutines.tasks.await
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching

/**
 * [AuthTokenBackup] поверх Google Block Store. Без Google Play Services (кастомные прошивки
 * приставок) каждый вызов просто падает внутри — ошибка логируется и глотается.
 */
class BlockStoreAuthTokenBackup(
    context: Context,
    private val analyticsTracker: AnalyticsTracker,
) : AuthTokenBackup {

    private val client by lazy { Blockstore.getClient(context.applicationContext) }

    override suspend fun save(token: String) {
        if (token.isBlank()) return
        runSuspendCatching {
            val data = StoreBytesData.Builder()
                .setKey(TOKEN_KEY)
                .setBytes(token.toByteArray(Charsets.UTF_8))
                .setShouldBackupToCloud(true)
                .build()
            client.storeBytes(data).await()
        }.onFailure { error ->
            analyticsTracker.log(TAG, error) { "storeBytes failed" }
        }
    }

    override suspend fun restore(): String? =
        runSuspendCatching {
            val request = RetrieveBytesRequest.Builder()
                .setKeys(listOf(TOKEN_KEY))
                .build()
            client.retrieveBytes(request).await()
                .blockstoreDataMap[TOKEN_KEY]
                ?.bytes
                ?.toString(Charsets.UTF_8)
                ?.takeIf { it.isNotBlank() }
        }.getOrElse { error ->
            analyticsTracker.log(TAG, error) { "retrieveBytes failed" }
            null
        }

    override suspend fun clear() {
        runSuspendCatching {
            val request = DeleteBytesRequest.Builder()
                .setKeys(listOf(TOKEN_KEY))
                .build()
            client.deleteBytes(request).await()
        }.onFailure { error ->
            analyticsTracker.log(TAG, error) { "deleteBytes failed" }
        }
    }

    private companion object {
        const val TAG = "AuthTokenBackup"
        const val TOKEN_KEY = "yani_refresh_token"
    }
}
