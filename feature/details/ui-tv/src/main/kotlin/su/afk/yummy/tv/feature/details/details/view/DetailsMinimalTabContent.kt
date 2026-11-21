package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.feature.comments.CommentsViewModel
import su.afk.yummy.tv.feature.comments.tv.CommentsTvScreen
import su.afk.yummy.tv.feature.details.collections.view.CollectionsGrid
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.model.DetailsMinimalTab
import su.afk.yummy.tv.feature.details.episodes.EpisodesTvScreen
import su.afk.yummy.tv.feature.details.episodes.EpisodesViewModel
import su.afk.yummy.tv.feature.details.full.view.FullDetailsBody
import su.afk.yummy.tv.feature.details.rating.RatingTvScreen
import su.afk.yummy.tv.feature.details.rating.RatingViewModel
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsTvScreen
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsViewModel
import su.afk.yummy.tv.feature.details.similar.SimilarTvScreen
import su.afk.yummy.tv.feature.details.similar.SimilarViewModel
import su.afk.yummy.tv.feature.details.trailers.TrailersTvScreen
import su.afk.yummy.tv.feature.details.trailers.TrailersViewModel
import su.afk.yummy.tv.feature.reviews.list.ReviewsListTvScreen
import su.afk.yummy.tv.feature.reviews.list.ReviewsListViewModel

/**
 * Содержимое вкладки упрощённой деталки. Где у пункта есть свой экран, показывается он же во встроенном режиме
 * (`embedded = true`: без своего «Назад», фона и автофокуса) со своей ViewModel. ViewModel создаётся, когда вкладку
 * впервые показали, и живёт в записи деталки — при возврате на вкладку данные не грузятся заново.
 */
@Composable
internal fun DetailsMinimalTabContent(
    tab: DetailsMinimalTab,
    state: DetailsState.State,
    details: AnimeDetails,
    onEvent: (DetailsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animeId = details.id
    Box(modifier = modifier.fillMaxSize()) {
        when (tab) {
            DetailsMinimalTab.EPISODES -> {
                val vm = hiltViewModel<EpisodesViewModel, EpisodesViewModel.Factory>(
                    key = "minimal-episodes-$animeId",
                    creationCallback = { it.create(animeId, null) },
                )
                ScreenNavigator(vm) { s, e, ev -> EpisodesTvScreen(s, e, embedded = true, onEvent = ev) }
            }

            DetailsMinimalTab.INFO -> FullDetailsBody(
                details = details,
                onGenreSelected = { onEvent(DetailsState.Event.GenreSelected(it)) },
                onStudioSelected = { id, url -> onEvent(DetailsState.Event.StudioSelected(id, url)) },
                onDirectorSelected = { onEvent(DetailsState.Event.DirectorSelected(it)) },
            )

            DetailsMinimalTab.SCREENSHOTS -> {
                val vm = hiltViewModel<ScreenshotsViewModel, ScreenshotsViewModel.Factory>(
                    key = "minimal-screenshots-$animeId",
                    creationCallback = { it.create(animeId) },
                )
                ScreenNavigator(vm) { s, e, ev -> ScreenshotsTvScreen(s, e, embedded = true, onEvent = ev) }
            }

            DetailsMinimalTab.VIDEO -> {
                val vm = hiltViewModel<TrailersViewModel, TrailersViewModel.Factory>(
                    key = "minimal-trailers-$animeId",
                    creationCallback = { it.create(animeId) },
                )
                ScreenNavigator(vm) { s, e, ev -> TrailersTvScreen(s, e, embedded = true, onEvent = ev) }
            }

            DetailsMinimalTab.RATING -> {
                val vm = hiltViewModel<RatingViewModel, RatingViewModel.Factory>(
                    key = "minimal-rating-$animeId",
                    creationCallback = { it.create(animeId) },
                )
                ScreenNavigator(vm) { s, e, ev -> RatingTvScreen(s, e, embedded = true, onEvent = ev) }
            }

            // Тот же ряд-хронология, что в ленте обычной деталки.
            DetailsMinimalTab.RELATED -> DetailsUniverseRow(
                items = details.viewingOrder,
                currentAnimeId = animeId,
                onAnimeClick = { onEvent(DetailsState.Event.AnimeSelected(it)) },
                onShowAllClick = { onEvent(DetailsState.Event.ViewingOrderSelected) },
                modifier = Modifier.padding(top = 8.dp),
            )

            DetailsMinimalTab.SIMILAR -> {
                val vm = hiltViewModel<SimilarViewModel, SimilarViewModel.Factory>(
                    key = "minimal-similar-$animeId",
                    creationCallback = { it.create(animeId) },
                )
                ScreenNavigator(vm) { s, e, ev -> SimilarTvScreen(s, e, embedded = true, onEvent = ev) }
            }

            // Первая страница коллекций уже есть в деталке — отдельная загрузка не нужна.
            DetailsMinimalTab.COLLECTIONS -> CollectionsGrid(
                collections = state.collections,
                onCollectionSelected = { onEvent(DetailsState.Event.CollectionSelected(it)) },
            )

            DetailsMinimalTab.REVIEWS -> {
                val vm = hiltViewModel<ReviewsListViewModel, ReviewsListViewModel.Factory>(
                    key = "minimal-reviews-$animeId",
                    creationCallback = { it.create(animeId) },
                )
                ScreenNavigator(vm) { s, e, ev -> ReviewsListTvScreen(s, e, embedded = true, onEvent = ev) }
            }

            DetailsMinimalTab.COMMENTS -> {
                val vm = hiltViewModel<CommentsViewModel, CommentsViewModel.Factory>(
                    key = "minimal-comments-$animeId",
                    creationCallback = {
                        it.create(CommentTargetType.ANIME, animeId, details.commentsCount, false)
                    },
                )
                ScreenNavigator(vm) { s, e, ev -> CommentsTvScreen(s, e, embedded = true, onEvent = ev) }
            }
        }
    }
}
