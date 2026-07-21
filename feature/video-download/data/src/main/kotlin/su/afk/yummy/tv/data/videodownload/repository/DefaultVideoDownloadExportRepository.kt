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
import su.afk.yummy.tv.data.videodownload.worker.VideoExportWorker
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import javax.inject.Inject

class DefaultVideoDownloadExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val store: VideoDownloadStore,
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
        val metadata = context.contentResolver.query(
            parsedUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_FLAGS,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) to cursor.getLong(1) else null
        }
        check(
            metadata != null &&
                    metadata.second and
                    DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE.toLong() != 0L
        ) { context.getString(R.string.video_export_error_directory_read_only) }
        context.contentResolver.takePersistableUriPermission(
            parsedUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        val name = metadata.first.orEmpty().ifBlank { uri.toDirectoryFallbackName() }
        settingsStore.setVideoExportDirectory(uri, name)
        return VideoExportDestination(uri, name)
    }

    override suspend fun enqueue(
        downloadIds: List<Long>,
        destination: VideoExportDestination,
    ) {
        val workManager = WorkManager.getInstance(context)
        downloadIds.distinct().forEach { id ->
            val entry = store.dao.getById(id) ?: return@forEach
            if (entry.status != "Downloaded") return@forEach
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
    }

    override suspend fun cancel(downloadId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(downloadId)).await()
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
        fun uniqueWorkName(id: Long): String = "video_export_$id"
        fun workTag(id: Long): String = "video_export_tag_$id"
    }
}
