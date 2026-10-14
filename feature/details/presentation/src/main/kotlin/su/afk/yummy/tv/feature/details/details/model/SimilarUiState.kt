package su.afk.yummy.tv.feature.details.details.model

import kotlinx.collections.immutable.ImmutableList
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.text.UiText

sealed interface SimilarUiState {
    data object Loading : SimilarUiState
    data object Empty : SimilarUiState
    data class Error(val message: UiText) : SimilarUiState
    data class Content(val items: ImmutableList<AnimeRecommendation>) : SimilarUiState
}
