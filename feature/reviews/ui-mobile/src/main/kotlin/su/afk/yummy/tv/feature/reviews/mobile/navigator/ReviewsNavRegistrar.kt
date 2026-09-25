package su.afk.yummy.tv.feature.reviews.mobile.navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.mobile.MobileDetailPlaceholder
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.scene.listPaneAnchor
import su.afk.yummy.tv.feature.reviews.IMobileReviewsEntry
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsViewModel
import su.afk.yummy.tv.feature.reviews.list.ReviewsListViewModel
import su.afk.yummy.tv.feature.reviews.mobile.R
import su.afk.yummy.tv.feature.reviews.mobile.details.ReviewDetailsMobileScreen
import su.afk.yummy.tv.feature.reviews.mobile.list.ReviewsListMobileScreen
import su.afk.yummy.tv.feature.reviews.navigator.AnimeReviewsDestination
import su.afk.yummy.tv.feature.reviews.navigator.ReviewDetailsDestination
import su.afk.yummy.tv.feature.reviews.navigator.ReviewsDestination
import javax.inject.Inject

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class ReviewsNavRegistrar @Inject constructor() : IMobileReviewsEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<ReviewsDestination>(metadata = listPaneMetadata()) {
                val vm =
                    hiltViewModel<ReviewsListViewModel, ReviewsListViewModel.Factory>(key = "reviews-feed") {
                        it.create(null)
                    }; ScreenNavigator(vm) { state, effect, events ->
                ReviewsListMobileScreen(state, effect, events)
            }
            }
            entry<AnimeReviewsDestination>(metadata = listPaneMetadata()) { dest ->
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
            entry<ReviewDetailsDestination>(metadata = ListDetailSceneStrategy.detailPane(LIST_DETAIL_SCENE_KEY)) { dest ->
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

    private fun listPaneMetadata() = listPaneAnchor() + ListDetailSceneStrategy.listPane(
        sceneKey = LIST_DETAIL_SCENE_KEY,
        detailPlaceholder = {
            MobileDetailPlaceholder(
                icon = Icons.Outlined.RateReview,
                text = stringResource(R.string.reviews_mobile_detail_placeholder),
            )
        },
    )
}

private const val LIST_DETAIL_SCENE_KEY = "reviews"
