package su.afk.yummy.tv.core.storage.watchprogress

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.di.StorageModule
import java.io.File

class WatchProgressBackupAgent : BackupAgent() {
    private val snapshotFile get() = File(filesDir, "watch-progress-backup.json")
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Готовит снимок перед обычным файловым бэкапом Android. */
    override fun onFullBackup(data: FullBackupDataOutput) {
        val file = snapshotFile
        try {
            val snapshot = withHistory { dao ->
                val entries = dao.latestForBackup()
                Snapshot(1, entries, dao.suppressionsFor(entries.map { it.animeId }.distinct()))
            }
            file.writeText(json.encodeToString(snapshot))
            super.onFullBackup(data)
        } finally {
            file.delete()
        }
    }

    /** Объединяет восстановленную историю с локальной в одной транзакции. */
    override fun onRestoreFinished() {
        super.onRestoreFinished()
        val file = snapshotFile
        if (!file.isFile) return
        try {
            val snapshot = json.decodeFromString<Snapshot>(file.readText())
            if (snapshot.version != 1 || snapshot.entries.size > 100 || snapshot.suppressions.size > 100) return
            withHistory { dao ->
                for (entry in snapshot.entries) {
                    val local = dao.get(entry.animeId, entry.episode)
                    if (local == null || local.updatedAt < entry.updatedAt) dao.save(entry)
                }
                val ids = snapshot.entries.map { it.animeId }.toSet()
                val local = dao.suppressionsFor(ids.toList()).associateBy { it.animeId }
                for (entry in snapshot.suppressions) {
                    val previous = local[entry.animeId]
                    if (entry.animeId in ids && (previous == null || previous.suppressedAt < entry.suppressedAt)) {
                        dao.saveSuppression(entry)
                    }
                }
            }
        } catch (error: SerializationException) {
            Log.w("WatchProgressBackup", "Invalid watch progress snapshot", error)
        } finally {
            file.delete()
        }
    }

    /** В режиме системного восстановления Android может запустить агент без Hilt Application. */
    private fun <T> withHistory(block: suspend (WatchProgressDao) -> T): T = runBlocking(Dispatchers.IO) {
        val db = StorageModule.provideAppDatabase(this@WatchProgressBackupAgent)
        try {
            db.withTransaction { block(db.watchProgressDao()) }
        } finally {
            db.close()
        }
    }

    // При fullBackupOnly Android использует файловые колбэки агента.
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput, newState: ParcelFileDescriptor) = Unit

    override fun onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor) = Unit

    @Serializable
    private data class Snapshot(
        val version: Int,
        val entries: List<WatchProgressEntry>,
        val suppressions: List<ContinueWatchingSuppressionEntry>,
    )
}
