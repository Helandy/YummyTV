package su.afk.yummy.tv.domain.account

/** Syncs a batch of remote watch states with the account API. */
class SyncWatchedVideosUseCase(private val repository: VideoWatchesRepository) {
    suspend operator fun invoke(states: List<RemoteWatchState>): Boolean = repository.syncWatched(states)
}
