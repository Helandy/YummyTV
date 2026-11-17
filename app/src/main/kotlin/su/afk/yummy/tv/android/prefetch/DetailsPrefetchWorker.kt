package su.afk.yummy.tv.android.prefetch

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveLibraryNewEpisodeAnimeIdsUseCase

/**
 * Раз в полдня на безлимитной сети подгружает в локальный кэш карточки тайтлов, к которым пользователь вернётся: «Продолжить
 * просмотр» и выбранные в настройках списки (не больше [LIMIT]). Если yani в этот момент недоступен, карточки приходят из
 * резервной копии YummyTV, и деталка потом открывается без сети. Видео не подгружаются: ссылки должны быть свежими.
 * Сбой одного тайтла не прерывает остальные; воркер завершается успешно, повтор по расписанию.
 */
@HiltWorker
class DetailsPrefetchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsStore: SettingsStore,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
    private val observeLibraryNewEpisodeAnimeIds: ObserveLibraryNewEpisodeAnimeIdsUseCase,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ids = runSuspendCatching {
            val continueIds = observeContinueWatching().first().map { it.animeId }
            val libraryIds = observeLibraryNewEpisodeAnimeIds(settingsStore.newEpisodesSources.first()).first()
            (continueIds + libraryIds).distinct().take(LIMIT)
        }.getOrElse { return Result.success() }

        ids.forEach { id ->
            runSuspendCatching { getAnimeDetails(id) }
            delay(PAUSE_MS)
        }
        return Result.success()
    }

    private companion object {
        const val LIMIT = 30
        const val PAUSE_MS = 500L
    }
}
