package su.afk.yummy.tv.android.startup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.BuildConfig
import su.afk.yummy.tv.android.image.ImageFormatAnalytics
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.settings.ImageFormatSupport
import su.afk.yummy.tv.core.network.yani.YaniContactTracker
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.maintenance.StorageCleanup
import su.afk.yummy.tv.core.utils.coroutines.di.DefaultApplicationScope
import su.afk.yummy.tv.data.videodownload.cache.LegacyStreamingCachePruner
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Разовые фоновые задачи холодного старта, не блокирующие onCreate: языковые настройки,
 * чистка APK после самообновления и устаревших/legacy-кэшей.
 */
@Singleton
class AppStartupMaintenanceRunner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val legacyStreamingCachePruner: LegacyStreamingCachePruner,
    private val storageCleanupStore: StorageCleanup,
    private val yaniContactTracker: YaniContactTracker,
    private val imageFormatSupport: ImageFormatSupport,
    private val imageFormatAnalytics: ImageFormatAnalytics,
    @DefaultApplicationScope private val scope: CoroutineScope,
) {

    // Нужно знать, у кого есть AVIF: от этого зависят качество постеров и доля трафика с нашего хоста.
    private suspend fun reportImageFormats() {
        imageFormatAnalytics.eventImageFormats(
            avifSupported = imageFormatSupport.isAvifSupported(),
        )
    }

    fun run() {
        scope.launch {
            // Фоновая обслуживающая задача не должна ронять процесс (диск полон, битый файл настроек).
            runSuspendCatching {
                settingsStore.ensureYaniContentLanguageInitialized()
                if (settingsStore.markStartedVersion(BuildConfig.VERSION_CODE)) {
                    deleteDownloadedUpdateApk()
                }
            }
            // Без одноразового флага: pruner сам отказывается работать, пока жива хоть одна
            // загрузка со старой схемой ключей, — зато подберёт их данные, как только не останется.
            runSuspendCatching { legacyStreamingCachePruner.pruneOrphanedEntries() }
            purgeStaleCachesAfterYaniResponds()
            runSuspendCatching { reportImageFormats() }
        }
    }

    /**
     * Плановая чистка кэша идёт только после первого ответа yani в этом запуске. Пока yani недоступен, кэш — единственный источник
     * данных: старше недели он или нет, удалять его нельзя (иначе при долгом сбое пользователь остаётся ни с чем). Ответы
     * резервной копии YummyTV не считаются.
     */
    private fun purgeStaleCachesAfterYaniResponds() {
        scope.launch {
            runSuspendCatching {
                yaniContactTracker.awaitContact()
                storageCleanupStore.purgeStaleCaches()
            }
        }
    }

    private fun deleteDownloadedUpdateApk() {
        File(context.cacheDir, UPDATE_DIR_NAME).deleteRecursively()
        // Раньше APK лежал прямо в cacheDir.
        File(context.cacheDir, UPDATE_APK_FILE_NAME).delete()
    }

    private companion object {
        const val UPDATE_DIR_NAME = "update"
        const val UPDATE_APK_FILE_NAME = "update.apk"
    }
}
