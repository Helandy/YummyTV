package su.afk.yummy.tv.core.storage.outbox

/**
 * Triggers a flush of the [PendingMutationOutbox] as soon as the network allows it. Declared here
 * (not in `:app`, where the actual `WorkManager` job lives) so presentation-layer callers that
 * enqueue a mutation don't need a dependency on `:app`.
 */
interface PendingMutationSyncScheduler {
    fun scheduleFlush()
}
