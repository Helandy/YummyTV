package su.afk.yummy.tv.core.storage.outbox

import kotlinx.coroutines.flow.Flow

internal class PendingMutationStore(
    private val dao: PendingMutationDao,
) : PendingMutationOutbox {

    override suspend fun enqueue(type: String, payloadJson: String) {
        dao.insert(PendingMutationEntry(type = type, payloadJson = payloadJson))
    }

    override suspend fun pending(): List<PendingMutationEntry> = dao.all()

    override suspend fun remove(id: Long) = dao.delete(id)

    override suspend fun recordAttemptFailure(id: Long) = dao.incrementAttempt(id)

    override fun observeCount(): Flow<Int> = dao.observeCount()
}
