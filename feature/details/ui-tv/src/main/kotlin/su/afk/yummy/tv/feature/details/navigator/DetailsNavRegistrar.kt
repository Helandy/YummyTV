package su.afk.yummy.tv.feature.details.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.details.collections.CollectionsTvScreen
import su.afk.yummy.tv.feature.details.collections.CollectionsViewModel
import su.afk.yummy.tv.feature.details.details.DetailsTvScreen
import su.afk.yummy.tv.feature.details.details.DetailsViewModel
import su.afk.yummy.tv.feature.details.episodes.EpisodesTvScreen
import su.afk.yummy.tv.feature.details.episodes.EpisodesViewModel
import su.afk.yummy.tv.feature.details.full.FullDetailsTvScreen
import su.afk.yummy.tv.feature.details.full.FullDetailsViewModel
import su.afk.yummy.tv.feature.details.navigator.DetailsCollectionsDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsEpisodesDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsFullDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsRatingDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsRelationDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsScreenshotsDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsSimilarDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsSubscriptionsDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsTrailersDestination
import su.afk.yummy.tv.feature.details.navigator.DetailsViewingOrderDestination
import su.afk.yummy.tv.feature.details.rating.RatingTvScreen
import su.afk.yummy.tv.feature.details.rating.RatingViewModel
import su.afk.yummy.tv.feature.details.relation.RelationTvScreen
import su.afk.yummy.tv.feature.details.relation.RelationViewModel
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsTvScreen
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsViewModel
import su.afk.yummy.tv.feature.details.similar.SimilarTvScreen
import su.afk.yummy.tv.feature.details.similar.SimilarViewModel
import su.afk.yummy.tv.feature.details.subscriptions.SubscriptionsTvScreen
import su.afk.yummy.tv.feature.details.subscriptions.SubscriptionsViewModel
import su.afk.yummy.tv.feature.details.trailers.TrailersTvScreen
import su.afk.yummy.tv.feature.details.trailers.TrailersViewModel
import su.afk.yummy.tv.feature.details.viewingorder.ViewingOrderTvScreen
import su.afk.yummy.tv.feature.details.viewingorder.ViewingOrderViewModel
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

            entry<DetailsSubscriptionsDestination> { dest ->
                val viewModel =
                    hiltViewModel<SubscriptionsViewModel, SubscriptionsViewModel.Factory>(
                        key = "subscriptions-${dest.animeId}",
                        creationCallback = { factory -> factory.create(dest.animeId) },
                    )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SubscriptionsTvScreen(state = state, effect = effect, onEvent = onEvent)
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

            entry<DetailsRatingDestination> { dest ->
                val viewModel = hiltViewModel<RatingViewModel, RatingViewModel.Factory>(
                    key = "rating-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    RatingTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }

            entry<DetailsCollectionsDestination> { dest ->
                val viewModel = hiltViewModel<CollectionsViewModel, CollectionsViewModel.Factory>(
                    key = "collections-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CollectionsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<DetailsRelationDestination> { dest ->
                val viewModel = hiltViewModel<RelationViewModel, RelationViewModel.Factory>(
                    key = "relation-${dest.kind}-${dest.id}-${dest.url}",
                    creationCallback = { factory -> factory.create(dest.kind, dest.id, dest.url) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    RelationTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
