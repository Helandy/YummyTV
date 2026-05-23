package su.afk.yummy.tv.feature.details.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.details.DetailsTvScreen
import su.afk.yummy.tv.feature.details.DetailsViewModel
import su.afk.yummy.tv.feature.details.EpisodesTvScreen
import su.afk.yummy.tv.feature.details.EpisodesViewModel
import su.afk.yummy.tv.feature.details.FullDetailsTvScreen
import su.afk.yummy.tv.feature.details.FullDetailsViewModel
import su.afk.yummy.tv.feature.details.ScreenshotsTvScreen
import su.afk.yummy.tv.feature.details.ScreenshotsViewModel
import su.afk.yummy.tv.feature.details.SimilarTvScreen
import su.afk.yummy.tv.feature.details.SimilarViewModel
import su.afk.yummy.tv.feature.details.TrailersTvScreen
import su.afk.yummy.tv.feature.details.TrailersViewModel
import su.afk.yummy.tv.feature.details.ViewingOrderTvScreen
import su.afk.yummy.tv.feature.details.ViewingOrderViewModel
import javax.inject.Inject

class DetailsNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<DetailsDestination> { dest ->
                val viewModel = hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(
                    key = "details-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    DetailsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }

            entry<DetailsFullDestination> { dest ->
                val viewModel = hiltViewModel<FullDetailsViewModel, FullDetailsViewModel.Factory>(
                    key = "full-details-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    FullDetailsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }

            entry<DetailsEpisodesDestination> { dest ->
                val viewModel = hiltViewModel<EpisodesViewModel, EpisodesViewModel.Factory>(
                    key = "episodes-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    EpisodesTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }

            entry<DetailsTrailersDestination> { dest ->
                val viewModel = hiltViewModel<TrailersViewModel, TrailersViewModel.Factory>(
                    key = "trailers-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    TrailersTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }

            entry<DetailsSimilarDestination> { dest ->
                val viewModel = hiltViewModel<SimilarViewModel, SimilarViewModel.Factory>(
                    key = "similar-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SimilarTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }

            entry<DetailsViewingOrderDestination> { dest ->
                val viewModel = hiltViewModel<ViewingOrderViewModel, ViewingOrderViewModel.Factory>(
                    key = "viewing-order-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ViewingOrderTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }

            entry<DetailsScreenshotsDestination> { dest ->
                val viewModel = hiltViewModel<ScreenshotsViewModel, ScreenshotsViewModel.Factory>(
                    key = "screenshots-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ScreenshotsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
