package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.model.MobileDetailsAction
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun buildMobileActions(
    state: DetailsState.State,
    details: AnimeDetails,
    onSubscriptionsSelected: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    onRatingScreenSelected: () -> Unit,
    onCollectionsSelected: () -> Unit,
): List<MobileDetailsAction> {
    val availableActions = buildList {
        add(
            MobileDetailsAction(
                DetailsButtonAction.EPISODES,
                stringResource(R.string.details_mobile_episodes),
                Icons.Filled.VideoLibrary,
                onEpisodesSelected,
            )
        )
        if (state.isSignedIn) {
            add(
                MobileDetailsAction(
                    DetailsButtonAction.SUBSCRIPTIONS,
                    stringResource(R.string.details_mobile_subscriptions),
                    Icons.Filled.Notifications,
                    onSubscriptionsSelected,
                )
            )
        }
        add(
            MobileDetailsAction(
                DetailsButtonAction.FULL_DETAILS,
                stringResource(R.string.details_mobile_full_details),
                Icons.Filled.Info,
                onFullDetailsSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.TRAILERS,
                stringResource(R.string.details_mobile_trailers),
                Icons.Filled.Movie,
                onTrailersSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.SIMILAR,
                stringResource(R.string.details_mobile_similar),
                Icons.Filled.AutoAwesome,
                onSimilarSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.VIEWING_ORDER,
                stringResource(R.string.details_mobile_viewing_order),
                Icons.Filled.FormatListNumbered,
                onViewingOrderSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.RATING,
                stringResource(R.string.details_mobile_rating),
                Icons.Filled.Star,
                onRatingScreenSelected,
            )
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.COLLECTIONS,
                stringResource(R.string.details_mobile_collections),
                Icons.Filled.CollectionsBookmark,
                onCollectionsSelected,
            )
        )
        if (details.screenshots.isNotEmpty()) {
            add(
                MobileDetailsAction(
                    DetailsButtonAction.SCREENSHOTS,
                    stringResource(R.string.details_mobile_screenshots),
                    Icons.Filled.PhotoLibrary,
                    onScreenshotsSelected,
                )
            )
        }
    }
    val byAction = availableActions.associateBy { it.action }
    return state.detailsButtonOrder
        .filterNot { it == DetailsButtonAction.WATCH || it == DetailsButtonAction.LIBRARY || it == DetailsButtonAction.FAVORITE }
        .mapNotNull { byAction[it] } +
            availableActions.filterNot { it.action in state.detailsButtonOrder }
}
