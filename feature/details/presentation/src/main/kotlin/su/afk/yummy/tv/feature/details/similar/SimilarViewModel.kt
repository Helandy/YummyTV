package su.afk.yummy.tv.feature.details.similar

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeRecommendationsUseCase
import su.afk.yummy.tv.domain.anime.usecase.SetAnimeRecommendationIgnoredUseCase
import su.afk.yummy.tv.domain.anime.usecase.VoteAnimeRecommendationUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.model.SimilarUiState
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.similar.utils.updateItem

@HiltViewModel(assistedFactory = SimilarViewModel.Factory::class)
class SimilarViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeRecommendations: GetAnimeRecommendationsUseCase,
    private val setAnimeRecommendationIgnored: SetAnimeRecommendationIgnoredUseCase,
    private val voteAnimeRecommendation: VoteAnimeRecommendationUseCase,
    private val settingsStore: YaniAccountSettingsStore,
    private val stringProvider: StringProvider,
    private val analytics: DetailsAnalytics,
) : BaseViewModel<SimilarState.State, SimilarState.Event, SimilarState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): SimilarViewModel
    }

    override fun createInitialState() = SimilarState.State()

    init {
        analytics.eventSimilarScreenOpened(animeId)
        // Скрытие живёт в сторе и переживает перезапуск — кнопка должна показывать его состояние.
        settingsStore.hiddenRecommendationIds
            .onEach { hiddenIds ->
                if (!currentState.isRecommendationMutationPending) {
                    setState { copy(isRecommendationIgnored = animeId in hiddenIds) }
                }
            }
            .launchIn(viewModelScope)
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: SimilarState.Event) {
        when (event) {
            SimilarState.Event.BackSelected -> nav.back()
            is SimilarState.Event.AnimeSelected -> {
                analytics.eventSimilarAnimeSelected(animeId, event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            is SimilarState.Event.SourceSelected -> selectSource(event.fromAi)
            SimilarState.Event.SourceToggled -> selectSource(!currentState.fromAi)
            SimilarState.Event.RetrySelected -> viewModelScope.launch { load() }
            SimilarState.Event.RecommendationVisibilityToggled -> toggleRecommendationVisibility()
            is SimilarState.Event.VoteSelected -> vote(event.similarAnimeId, event.vote)
        }
    }

    private fun selectSource(fromAi: Boolean) {
        if (currentState.fromAi == fromAi) return
        analytics.eventSimilarSourceSelected(animeId, fromAi)
        setState { copy(fromAi = fromAi) }
        viewModelScope.launch { load(fromAi) }
    }

    private suspend fun load(fromAi: Boolean = currentState.fromAi) {
        setState { copy(similarState = SimilarUiState.Loading) }
        runCatching { getAnimeRecommendations(animeId, fromAi) }.fold(
            // Выдача (особенно AI) может повторить тайтл — в lazy-списке это дубль ключа и краш.
            onSuccess = { loaded ->
                val items = loaded.distinctBy { it.animeId }
                setState {
                    if (this.fromAi == fromAi) {
                        val nextState = if (items.isEmpty()) {
                            SimilarUiState.Empty
                        } else {
                            SimilarUiState.Content(items.toImmutableList())
                        }
                        copy(similarState = nextState)
                    } else {
                        this
                    }
                }
            },
            onFailure = {
                setState {
                    if (this.fromAi == fromAi) {
                        copy(similarState = SimilarUiState.Error(it.message))
                    } else {
                        this
                    }
                }
            },
        )
    }

    private fun toggleRecommendationVisibility() {
        if (currentState.isRecommendationMutationPending) return
        viewModelScope.launch {
            if (!canMutate()) return@launch
            val previous = currentState.isRecommendationIgnored
            val target = !previous
            setState {
                copy(
                    isRecommendationIgnored = target,
                    isRecommendationMutationPending = true,
                )
            }
            runCatching { setAnimeRecommendationIgnored(animeId, target) }.fold(
                onSuccess = { success ->
                    if (!success) {
                        setState { copy(isRecommendationIgnored = previous) }
                        showMutationError()
                    }
                    setState { copy(isRecommendationMutationPending = false) }
                },
                onFailure = {
                    setState {
                        copy(
                            isRecommendationIgnored = previous,
                            isRecommendationMutationPending = false,
                        )
                    }
                    showMutationError()
                },
            )
        }
    }

    private fun vote(similarAnimeId: Int, selectedVote: AnimeRecommendationVote) {
        if (similarAnimeId in currentState.pendingVoteAnimeIds) return
        val currentItem = currentState.contentItems()
            .firstOrNull { it.animeId == similarAnimeId } ?: return
        val targetVote = if (currentItem.vote == selectedVote) {
            AnimeRecommendationVote.NONE
        } else {
            selectedVote
        }
        viewModelScope.launch {
            if (!canMutate()) return@launch
            setState {
                copy(
                    similarState = similarState.updateItem(
                        currentItem.optimistic(targetVote)
                    ),
                    pendingVoteAnimeIds = pendingVoteAnimeIds + similarAnimeId,
                )
            }
            runCatching { voteAnimeRecommendation(animeId, similarAnimeId, targetVote) }.fold(
                onSuccess = { reaction ->
                    setState {
                        if (!fromAi) {
                            copy(
                                similarState = similarState.updateItem(
                                    currentItem.copy(
                                        likes = reaction.likes,
                                        dislikes = reaction.dislikes,
                                        vote = reaction.vote,
                                    )
                                ),
                                pendingVoteAnimeIds = pendingVoteAnimeIds - similarAnimeId,
                            )
                        } else {
                            copy(pendingVoteAnimeIds = pendingVoteAnimeIds - similarAnimeId)
                        }
                    }
                },
                onFailure = {
                    setState {
                        if (!fromAi) {
                            copy(
                                similarState = similarState.updateItem(currentItem),
                                pendingVoteAnimeIds = pendingVoteAnimeIds - similarAnimeId,
                            )
                        } else {
                            copy(pendingVoteAnimeIds = pendingVoteAnimeIds - similarAnimeId)
                        }
                    }
                    showMutationError()
                },
            )
        }
    }

    private suspend fun canMutate(): Boolean {
        if (settingsStore.yaniUserId.first() > 0) return true
        setEffect(
            SimilarState.Effect.ShowToast(
                stringProvider.get(R.string.details_similar_auth_required)
            )
        )
        return false
    }

    private fun showMutationError() {
        setEffect(
            SimilarState.Effect.ShowToast(
                stringProvider.get(R.string.details_similar_mutation_error)
            )
        )
    }

    private fun SimilarState.State.contentItems(): List<AnimeRecommendation> =
        (similarState as? SimilarUiState.Content)?.items.orEmpty()

}
