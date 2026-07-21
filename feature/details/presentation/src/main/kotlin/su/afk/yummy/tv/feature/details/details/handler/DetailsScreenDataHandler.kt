package su.afk.yummy.tv.feature.details.details.handler

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.runSuspendCatching
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeWatchProgressUseCase
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.domain.library.usecase.AnimeLibraryState
import su.afk.yummy.tv.domain.library.usecase.ObserveAnimeLibraryStateUseCase
import su.afk.yummy.tv.domain.library.usecase.RefreshLibraryMetadataUseCase
import javax.inject.Inject

class DetailsScreenDataHandler @Inject constructor(
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val observeAnimeLibraryState: ObserveAnimeLibraryStateUseCase,
    private val observeAnimeWatchProgress: ObserveAnimeWatchProgressUseCase,
    private val refreshLibraryMetadata: RefreshLibraryMetadataUseCase,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val settingsStore: SettingsStore,
) {

    val preferredPlayer: Flow<PreferredPlayer> = settingsStore.preferredPlayer
    val yaniUserId: Flow<Int> = settingsStore.yaniUserId
    val detailsButtonOrder: Flow<List<DetailsButtonAction>> = settingsStore.detailsButtonOrder

    fun observeLibraryState(animeId: Int): Flow<AnimeLibraryState> =
        observeAnimeLibraryState(animeId)

    fun observeWatchProgress(animeId: Int): Flow<List<AnimeWatchProgress>> =
        observeAnimeWatchProgress(animeId)

    fun observeAccountSession(): Flow<AccountSession> = observeAccountSession.invoke()

    suspend fun loadDetails(animeId: Int): Result<AnimeDetails> =
        runSuspendCatching { getAnimeDetails(animeId) }

    suspend fun refreshLibraryMetadata(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
    ) {
        refreshLibraryMetadata.invoke(animeId, title, poster)
    }
}
