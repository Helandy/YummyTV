package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.MigrateContinueWatchingProgressUseCase
import javax.inject.Inject

/** Resolves the source and resume position for a Continue Watching launch. */
class ResolveContinueWatchingLaunchUseCase @Inject internal constructor(
    private val videoLoader: ContinueWatchingVideoLoader,
    private val launchResolver: ContinueWatchingLaunchResolver,
    private val migrateContinueWatchingProgress: MigrateContinueWatchingProgressUseCase,
) {

    suspend operator fun invoke(
        entry: HomeContinueWatchingItem,
        refreshProgressOnLaunch: Boolean,
    ): ContinueWatchingLaunch {
        val availableVideos = videoLoader.load(
            animeId = entry.animeId,
            refresh = refreshProgressOnLaunch,
        )
        val resolution = launchResolver.resolve(
            entry = entry,
            availableVideos = availableVideos,
            useServerProgress = refreshProgressOnLaunch,
        )
        resolution.progressMigration?.let { migration ->
            migrateContinueWatchingProgress(migration)
        }
        return resolution.launch
    }
}
