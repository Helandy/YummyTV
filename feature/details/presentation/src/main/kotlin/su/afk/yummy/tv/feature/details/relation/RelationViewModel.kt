package su.afk.yummy.tv.feature.details.relation

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.anime.model.AnimeRelationKind
import su.afk.yummy.tv.domain.anime.model.AnimeRelationReference
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeRelationUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.navigator.DetailsRelationKind
import su.afk.yummy.tv.feature.details.relation.model.RelationType

@HiltViewModel(assistedFactory = RelationViewModel.Factory::class)
class RelationViewModel @AssistedInject internal constructor(
    @Assisted private val kind: DetailsRelationKind,
    @Assisted private val id: Int,
    @Assisted private val url: String?,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeRelation: GetAnimeRelationUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModel<RelationState.State, RelationState.Event, RelationState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(kind: DetailsRelationKind, id: Int, url: String?): RelationViewModel
    }

    override fun createInitialState() = RelationState.State(
        relationType = when (kind) {
            DetailsRelationKind.STUDIO -> RelationType.STUDIO
            DetailsRelationKind.DIRECTOR -> RelationType.DIRECTOR
            DetailsRelationKind.GENRE -> RelationType.GENRE
        },
    )

    init {
        load()
    }

    override fun onEvent(event: RelationState.Event) {
        when (event) {
            RelationState.Event.BackSelected -> nav.back()
            RelationState.Event.RetrySelected -> load()
            is RelationState.Event.AnimeSelected ->
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))

            is RelationState.Event.SubGenreSelected ->
                nav.navigate(
                    detailsNavigator.getRelationDest(DetailsRelationKind.GENRE, event.id)
                )
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val reference = AnimeRelationReference(
                kind = when (kind) {
                    DetailsRelationKind.STUDIO -> AnimeRelationKind.STUDIO
                    DetailsRelationKind.DIRECTOR -> AnimeRelationKind.DIRECTOR
                    DetailsRelationKind.GENRE -> AnimeRelationKind.GENRE
                },
                id = id,
                url = url,
            )
            runSuspendCatching { getAnimeRelation(reference) }.fold(
                onSuccess = { setState { copy(isLoading = false, relation = it) } },
                onFailure = { error ->
                    setState {
                        copy(
                            isLoading = false,
                            error = errorHandler.parse(error).message,
                        )
                    }
                },
            )
        }
    }
}
