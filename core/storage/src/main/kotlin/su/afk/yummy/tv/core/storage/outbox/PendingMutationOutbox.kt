package su.afk.yummy.tv.core.storage.outbox

import kotlinx.coroutines.flow.Flow

/** Durable-очередь мутаций, не доехавших до сервера из-за офлайна. */
interface PendingMutationOutbox {

    suspend fun enqueue(type: String, payloadJson: String)

    suspend fun pending(): List<PendingMutationEntry>

    suspend fun remove(id: Long)

    suspend fun recordAttemptFailure(id: Long)

    fun observeCount(): Flow<Int>
}
