package su.afk.yummy.tv.feature.reviews.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsMobileScreen
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsViewModel
import su.afk.yummy.tv.feature.reviews.list.ReviewsListMobileScreen
import su.afk.yummy.tv.feature.reviews.list.ReviewsListViewModel
import su.afk.yummy.tv.feature.reviews.navigator.AnimeReviewsDestination
import su.afk.yummy.tv.feature.reviews.navigator.ReviewDetailsDestination
import su.afk.yummy.tv.feature.reviews.navigator.ReviewsDestination
import javax.inject.Inject

class ReviewsNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<ReviewsDestination> {
                val vm =
                    hiltViewModel<ReviewsListViewModel, ReviewsListViewModel.Factory>(key = "reviews-feed") {
                        it.create(null)
                    }; ScreenNavigator(vm) { state, effect, events ->
                ReviewsListMobileScreen(state, effect, events)
            }
            }
            entry<AnimeReviewsDestination> { dest ->
                val vm =
                    hiltViewModel<ReviewsListViewModel, ReviewsListViewModel.Factory>(key = "reviews-${dest.animeId}") {
                        it.create(dest.animeId)
                    }; ScreenNavigator(vm) { state, effect, events ->
                ReviewsListMobileScreen(
                    state,
                    effect,
                    events
                )
            }
            }
            entry<ReviewDetailsDestination> { dest ->
                val vm =
                    hiltViewModel<ReviewDetailsViewModel, ReviewDetailsViewModel.Factory>(key = "review-${dest.reviewId}") {
                        it.create(dest.reviewId)
                    }; ScreenNavigator(vm) { state, effect, events ->
                ReviewDetailsMobileScreen(
                    state,
                    effect,
                    events
                )
            }
            }
        }
}
