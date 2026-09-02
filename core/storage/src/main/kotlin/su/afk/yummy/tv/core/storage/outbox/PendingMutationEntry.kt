package su.afk.yummy.tv.core.storage.outbox

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Мутация, которая ушла в сеть при офлайне и ждёт повтора. [type] и [payloadJson] непрозрачны для
 * этого слоя — их читает/пишет только код, знающий о конкретном домене мутации (см.
 * `PendingMutationSyncWorker` в `:app`), `core:storage` лишь durable-хранит очередь.
 */
@Entity(tableName = "pending_mutations")
data class PendingMutationEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
)
