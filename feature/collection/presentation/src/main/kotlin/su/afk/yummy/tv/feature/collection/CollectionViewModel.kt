package su.afk.yummy.tv.feature.collection

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.domain.collection.usecase.GetCollectionUseCase
import su.afk.yummy.tv.domain.collection.usecase.RemoveCollectionVoteUseCase
import su.afk.yummy.tv.domain.collection.usecase.VoteCollectionUseCase
import su.afk.yummy.tv.feature.collection.presentation.R
import su.afk.yummy.tv.feature.details.IDetailsNavigator

@HiltViewModel(assistedFactory = CollectionViewModel.Factory::class)
class CollectionViewModel @AssistedInject internal constructor(
    @Assisted private val collectionId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getCollection: GetCollectionUseCase,
    private val getAccountSession: GetAccountSessionUseCase,
    private val voteCollection: VoteCollectionUseCase,
    private val removeCollectionVote: RemoveCollectionVoteUseCase,
    private val stringProvider: StringProvider,
    private val analytics: CollectionAnalytics,
) : BaseViewModelNew<CollectionState.State, CollectionState.Event, CollectionState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(collectionId: Int): CollectionViewModel
    }

    override fun createInitialState() = CollectionState.State()

    init {
        analytics.eventScreenOpened(collectionId)
        load()
    }

    override fun onEvent(event: CollectionState.Event) {
        when (event) {
            CollectionState.Event.BackSelected -> nav.back()
            CollectionState.Event.RetrySelected -> {
                analytics.eventRetry(collectionId)
                load()
            }
            is CollectionState.Event.AnimeSelected -> {
                analytics.eventAnimeSelected(collectionId, event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }
            is CollectionState.Event.VoteSelected -> vote(event.vote)
            is CollectionState.Event.GridScrolled -> setState {
                copy(firstVisibleItemIndex = event.index, firstVisibleItemScrollOffset = event.offset)
            }
        }
    }

    private fun vote(vote: CollectionVote) {
        if (vote == CollectionVote.NEUTRAL) return
        val collection = currentState.collection ?: return
        if (currentState.isVoteLoading) return
        viewModelScope.launch {
            if (!canVoteCollection()) return@launch
            setState { copy(isVoteLoading = true) }
            val currentVote = currentState.collection?.vote ?: CollectionVote.NEUTRAL
            runCatching {
                if (currentVote == vote) {
                    removeCollectionVote(collectionId) to CollectionVote.NEUTRAL
                } else {
                    voteCollection(collectionId, vote) to vote
                }
            }.fold(
                onSuccess = { (result, newVote) ->
                    setState {
                        copy(
                            isVoteLoading = false,
                            collection = collection.copy(
                                likesCount = result.likes,
                                dislikesCount = result.dislikes,
                                vote = newVote,
                            ),
                        )
                    }
                },
                onFailure = {
                    setState { copy(isVoteLoading = false) }
                    setEffect(
                        CollectionState.Effect.ShowToast(
                            stringProvider.get(R.string.collection_vote_error)
                        )
                    )
                },
            )
        }
    }

    private suspend fun canVoteCollection(): Boolean {
        val session = getAccountSession()
        if (session.isAuthorized && session.userId > 0) return true
        setEffect(
            CollectionState.Effect.ShowToast(
                stringProvider.get(R.string.collection_vote_auth_required)
            )
        )
        return false
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getCollection(collectionId) }.fold(
                onSuccess = { collection -> setState { copy(isLoading = false, collection = collection) } },
                onFailure = { e ->
                    analytics.eventLoadError(e)
                    setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.collection_load_error)) }
                },
            )
        }
    }
}
