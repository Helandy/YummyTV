package su.afk.yummy.tv.feature.reviews.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.reviews.ITvReviewsEntry
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsTvScreen
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsViewModel
import su.afk.yummy.tv.feature.reviews.list.ReviewsListTvScreen
import su.afk.yummy.tv.feature.reviews.list.ReviewsListViewModel
import su.afk.yummy.tv.feature.reviews.navigator.AnimeReviewsDestination
import su.afk.yummy.tv.feature.reviews.navigator.ReviewDetailsDestination
import su.afk.yummy.tv.feature.reviews.navigator.ReviewsDestination
import javax.inject.Inject

class ReviewsNavRegistrar @Inject constructor() : ITvReviewsEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<ReviewsDestination> {
                val vm =
                    hiltViewModel<ReviewsListViewModel, ReviewsListViewModel.Factory>(key = "tv-reviews-feed") {
                        it.create(null)
                    }; ScreenNavigator(vm) { state, effect, events ->
                ReviewsListTvScreen(state, effect, events)
            }
            }
            entry<AnimeReviewsDestination> { dest ->
                val vm =
                    hiltViewModel<ReviewsListViewModel, ReviewsListViewModel.Factory>(key = "tv-reviews-${dest.animeId}") {
                        it.create(dest.animeId)
                    }; ScreenNavigator(vm) { state, effect, events ->
                ReviewsListTvScreen(
                    state,
                    effect,
                    events
                )
            }
            }
            entry<ReviewDetailsDestination> { dest ->
                val vm =
                    hiltViewModel<ReviewDetailsViewModel, ReviewDetailsViewModel.Factory>(key = "tv-review-${dest.reviewId}") {
                        it.create(dest.reviewId)
                    }; ScreenNavigator(vm) { state, effect, events ->
                ReviewDetailsTvScreen(
                    state,
                    effect,
                    events
                )
            }
            }
        }
}
