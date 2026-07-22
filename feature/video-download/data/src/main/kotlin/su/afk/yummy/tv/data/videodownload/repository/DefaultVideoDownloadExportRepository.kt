package su.afk.yummy.tv.data.videodownload.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadStore
import su.afk.yummy.tv.data.videodownload.R
import su.afk.yummy.tv.data.videodownload.utils.treeDocumentUri
import su.afk.yummy.tv.data.videodownload.worker.VideoExportAnalytics
import su.afk.yummy.tv.data.videodownload.worker.VideoExportWorker
import su.afk.yummy.tv.data.videodownload.worker.logDownloadWarning
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.model.VideoExportSource
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import javax.inject.Inject

class DefaultVideoDownloadExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val store: VideoDownloadStore,
    private val analytics: VideoExportAnalytics,
) : VideoDownloadExportRepository {
    private val orphanReconciliationMutex = Mutex()

    override fun observeDestination(): Flow<VideoExportDestination?> =
        combine(
            settingsStore.videoExportDirectoryUri,
            settingsStore.videoExportDirectoryName,
        ) { uri, name ->
            uri.takeIf(String::isNotBlank)?.let {
                VideoExportDestination(
                    uri = it,
                    displayName = name.ifBlank { it.toDirectoryFallbackName() },
                )
            }
        }.onStart { reconcileOrphanedExports() }

    override suspend fun selectDestination(uri: String): VideoExportDestination {
        val parsedUri = Uri.parse(uri)
        val previousUri = settingsStore.videoExportDirectoryUri.first()
        // Права берём до запроса метаданных: временный грант из пикера живёт только до перезапуска
        context.contentResolver.takePersistableUriPermission(
            parsedUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        val metadata = runCatching { readDirectoryMetadata(parsedUri) }
            .onFailure { throwable ->
                logDownloadWarning(throwable) { "Failed to read export directory metadata" }
            }
            .getOrNull()
        if (metadata == null || !metadata.supportsCreate) {
            releasePersistablePermission(parsedUri)
            analytics.reportDirectoryRejected(
                reason = if (metadata == null) REASON_UNREADABLE else REASON_READ_ONLY,
            )
            error(context.getString(R.string.video_export_error_directory_read_only))
        }
        if (previousUri.isNotBlank() && previousUri != uri) {
            releasePersistablePermission(Uri.parse(previousUri))
        }
        val name = metadata.displayName.ifBlank { uri.toDirectoryFallbackName() }
        settingsStore.setVideoExportDirectory(uri, name)
        analytics.reportDirectorySelected()
        return VideoExportDestination(uri, name)
    }

    /** Метаданные читаются по document-URI папки: по tree-URI провайдер запрос не поддерживает. */
    private fun readDirectoryMetadata(treeUri: Uri): DirectoryMetadata? =
        context.contentResolver.query(
            treeUri.treeDocumentUri(),
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_FLAGS,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex =
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val flagsIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)
            val flags = cursor.getLong(flagsIndex)
            DirectoryMetadata(
                displayName = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "",
                supportsCreate = flags and
                        DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE.toLong() != 0L,
            )
        }

    override suspend fun isDestinationWritable(uri: String): Boolean {
        if (uri.isBlank()) return false
        val parsedUri = Uri.parse(uri)
        val hasPersistedWrite = context.contentResolver.persistedUriPermissions
            .any { it.uri == parsedUri && it.isWritePermission }
        if (!hasPersistedWrite) return false
        // Носитель могли извлечь, папку — удалить: проверяем, что каталог ещё отвечает
        return runCatching { readDirectoryMetadata(parsedUri) }
            .getOrNull()
            ?.supportsCreate == true
    }

    override suspend fun exportedFileExists(uri: String): Boolean {
        if (uri.isBlank()) return false
        return runCatching {
            context.contentResolver.query(
                Uri.parse(uri),
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            )?.use { cursor -> cursor.moveToFirst() }
        }.getOrNull() == true
    }

    private fun releasePersistablePermission(uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    private data class DirectoryMetadata(
        val displayName: String,
        val supportsCreate: Boolean,
    )

    override suspend fun enqueue(
        downloadIds: List<Long>,
        destination: VideoExportDestination,
        source: VideoExportSource,
    ) {
        val workManager = WorkManager.getInstance(context)
        var enqueuedCount = 0
        var firstItem: VideoDownloadItem? = null
        downloadIds.distinct().forEach { id ->
            val entry = store.dao.getById(id) ?: return@forEach
            if (entry.status != "Downloaded") return@forEach
            enqueuedCount += 1
            if (firstItem == null) firstItem = entry.toDomain()
            updateState(
                downloadId = id,
                status = VideoExportStatus.Queued,
                progress = 0f,
                destinationUri = destination.uri,
            )
            val request = OneTimeWorkRequestBuilder<VideoExportWorker>()
                .setInputData(
                    workDataOf(
                        VideoExportWorker.KEY_DOWNLOAD_ID to id,
                        VideoExportWorker.KEY_DESTINATION_URI to destination.uri,
                    )
                )
                .addTag(workTag(id))
                .build()
            workManager.enqueueUniqueWork(
                uniqueWorkName(id),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
        if (enqueuedCount > 0) analytics.reportEnqueued(enqueuedCount, source, firstItem)
    }

    override suspend fun enqueueAutoExportIfEnabled(downloadId: Long) {
        if (!settingsStore.videoExportAutoEnabled.first()) return
        val uri = settingsStore.videoExportDirectoryUri.first()
        if (uri.isBlank()) return
        if (!isDestinationWritable(uri)) {
            logDownloadWarning { "Auto export skipped: no access to export directory" }
            analytics.reportDirectoryRejected(reason = REASON_ACCESS_LOST)
            return
        }
        val name = settingsStore.videoExportDirectoryName.first()
            .ifBlank { uri.toDirectoryFallbackName() }
        enqueue(
            downloadIds = listOf(downloadId),
            destination = VideoExportDestination(uri, name),
            source = VideoExportSource.Auto,
        )
    }

    override suspend fun cancel(downloadId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(downloadId)).await()
        analytics.reportCancelled()
        updateState(
            downloadId = downloadId,
            status = VideoExportStatus.Idle,
            progress = 0f,
        )
    }

    override suspend fun updateState(
        downloadId: Long,
        status: VideoExportStatus,
        progress: Float?,
        destinationUri: String?,
        exportedFileUri: String?,
        errorMessage: String?,
    ) {
        val entry = store.dao.getById(downloadId) ?: return
        store.dao.update(
            entry.copy(
                exportStatus = status.name,
                exportProgress = progress?.coerceIn(0f, 1f) ?: entry.exportProgress,
                exportDirectoryUri = destinationUri ?: entry.exportDirectoryUri,
                exportedFileUri = if (status == VideoExportStatus.Exported) {
                    exportedFileUri
                } else {
                    null
                },
                exportErrorMessage = errorMessage,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun String.toDirectoryFallbackName(): String =
        Uri.decode(substringAfterLast('/')).ifBlank {
            context.getString(R.string.video_export_selected_directory)
        }

    private suspend fun reconcileOrphanedExports() = orphanReconciliationMutex.withLock {
        val workManager = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()
        store.dao.getUnfinishedExports()
            .filter { now - it.updatedAt >= ORPHAN_GRACE_PERIOD_MS }
            .forEach { entry ->
                val workInfos = runCatching {
                    workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(entry.id)).first()
                }.getOrNull() ?: return@forEach
                val hasActiveWork = workInfos.any { workInfo ->
                    workInfo.state == WorkInfo.State.ENQUEUED ||
                            workInfo.state == WorkInfo.State.RUNNING ||
                            workInfo.state == WorkInfo.State.BLOCKED
                }
                if (hasActiveWork) return@forEach
                updateState(
                    downloadId = entry.id,
                    status = VideoExportStatus.Failed,
                    errorMessage = context.getString(R.string.video_export_worker_stopped),
                )
            }
    }

    companion object {
        private const val ORPHAN_GRACE_PERIOD_MS = 60_000L
        private const val REASON_READ_ONLY = "read_only"
        private const val REASON_UNREADABLE = "unreadable"
        private const val REASON_ACCESS_LOST = "access_lost"
        fun uniqueWorkName(id: Long): String = "video_export_$id"
        fun workTag(id: Long): String = "video_export_tag_$id"
    }
}
