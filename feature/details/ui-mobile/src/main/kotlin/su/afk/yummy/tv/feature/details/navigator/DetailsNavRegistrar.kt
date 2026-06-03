package su.afk.yummy.tv.feature.details.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.details.collections.CollectionsMobileScreen
import su.afk.yummy.tv.feature.details.collections.CollectionsViewModel
import su.afk.yummy.tv.feature.details.details.DetailsMobileScreen
import su.afk.yummy.tv.feature.details.details.DetailsViewModel
import su.afk.yummy.tv.feature.details.episodes.EpisodesMobileScreen
import su.afk.yummy.tv.feature.details.episodes.EpisodesViewModel
import su.afk.yummy.tv.feature.details.full.FullDetailsMobileScreen
import su.afk.yummy.tv.feature.details.full.FullDetailsViewModel
import su.afk.yummy.tv.feature.details.rating.RatingMobileScreen
import su.afk.yummy.tv.feature.details.rating.RatingViewModel
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsMobileScreen
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsViewModel
import su.afk.yummy.tv.feature.details.similar.SimilarMobileScreen
import su.afk.yummy.tv.feature.details.similar.SimilarViewModel
import su.afk.yummy.tv.feature.details.trailers.TrailersMobileScreen
import su.afk.yummy.tv.feature.details.trailers.TrailersViewModel
import su.afk.yummy.tv.feature.details.viewingorder.ViewingOrderMobileScreen
import su.afk.yummy.tv.feature.details.viewingorder.ViewingOrderViewModel
import javax.inject.Inject

class DetailsNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<DetailsDestination> { dest ->
                val viewModel = hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(
                    key = "mobile-details-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    DetailsMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsFullDestination> { dest ->
                val viewModel = hiltViewModel<FullDetailsViewModel, FullDetailsViewModel.Factory>(
                    key = "mobile-full-details-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    FullDetailsMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsEpisodesDestination> { dest ->
                val viewModel = hiltViewModel<EpisodesViewModel, EpisodesViewModel.Factory>(
                    key = "mobile-episodes-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    EpisodesMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsTrailersDestination> { dest ->
                val viewModel = hiltViewModel<TrailersViewModel, TrailersViewModel.Factory>(
                    key = "mobile-trailers-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    TrailersMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsSimilarDestination> { dest ->
                val viewModel = hiltViewModel<SimilarViewModel, SimilarViewModel.Factory>(
                    key = "mobile-similar-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SimilarMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsViewingOrderDestination> { dest ->
                val viewModel = hiltViewModel<ViewingOrderViewModel, ViewingOrderViewModel.Factory>(
                    key = "mobile-viewing-order-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ViewingOrderMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsScreenshotsDestination> { dest ->
                val viewModel = hiltViewModel<ScreenshotsViewModel, ScreenshotsViewModel.Factory>(
                    key = "mobile-screenshots-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ScreenshotsMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsRatingDestination> { dest ->
                val viewModel = hiltViewModel<RatingViewModel, RatingViewModel.Factory>(
                    key = "mobile-rating-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    RatingMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsCollectionsDestination> { dest ->
                val viewModel = hiltViewModel<CollectionsViewModel, CollectionsViewModel.Factory>(
                    key = "mobile-collections-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CollectionsMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
