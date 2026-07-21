package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Replaces a legacy placeholder episode with its resolved playback episode. */
class MigrateContinueWatchingProgressUseCase @Inject constructor(
    private val repository: HomeFeedRepository,
) {
    suspend operator fun invoke(migration: ContinueWatchingProgressMigration) =
        repository.migrateContinueWatchingProgress(migration)
}
