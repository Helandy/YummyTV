package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.model.watching.ContinueWatchingLaunch
import su.afk.yummy.tv.domain.home.model.ContinueWatchingSource
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.MigrateContinueWatchingProgressUseCase
import su.afk.yummy.tv.domain.watching.utils.resolveContinueWatchingLaunch
import javax.inject.Inject

/**
 * Разрешает, какое видео и с какой позиции запускать по карточке «Продолжить просмотр».
 * Возвращает null, только если у карточки следующей серии ([ContinueWatchingSource.REMOTE_NEXT_EPISODE])
 * этой серии нет — тогда вызывающий открывает детали тайтла.
 */
class ResolveContinueWatchingLaunchUseCase @Inject internal constructor(
    private val loadContinueWatchingVideos: LoadContinueWatchingVideosUseCase,
    private val migrateContinueWatchingProgress: MigrateContinueWatchingProgressUseCase,
    private val resolveNextEpisodeLaunch: ResolveNextEpisodeLaunchUseCase,
) {

    suspend operator fun invoke(
        entry: HomeContinueWatchingItem,
        refreshProgressOnLaunch: Boolean,
    ): ContinueWatchingLaunch? {
        if (entry.source == ContinueWatchingSource.REMOTE_NEXT_EPISODE) {
            return resolveNextEpisodeLaunch(entry)
        }
        // Карточка из серверной истории всегда стартует с серверного прогресса, без оглядки на настройку.
        val useServerProgress =
            refreshProgressOnLaunch || entry.source == ContinueWatchingSource.REMOTE
        val availableVideos = loadContinueWatchingVideos(
            animeId = entry.animeId,
            refresh = useServerProgress,
        )
        val resolution = resolveContinueWatchingLaunch(
            entry = entry,
            availableVideos = availableVideos,
            useServerProgress = useServerProgress,
        )
        resolution.progressMigration?.let { migration ->
            migrateContinueWatchingProgress(migration)
        }
        return resolution.launch
    }
}
