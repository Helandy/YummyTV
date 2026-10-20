package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.mobile.details.model.MobileDetailsAction
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun buildMobileActions(
    state: DetailsState.State,
    details: AnimeDetails,
    onSubscriptionsSelected: () -> Unit,
    onSubscriptionsLockedSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onCollectionsSelected: () -> Unit,
    onReviewsSelected: () -> Unit,
): List<MobileDetailsAction> {
    // Трейлеры, видео блогеров, рейтинг и скриншоты на мобилке показаны блоками прямо в деталке.
    val availableActions = buildList {
        add(
            MobileDetailsAction(
                DetailsButtonAction.EPISODES,
                stringResource(R.string.details_episodes),
                Icons.Filled.VideoLibrary,
                onEpisodesSelected,
            ),
        )
        if (state.isSignedIn) {
            add(
                MobileDetailsAction(
                    DetailsButtonAction.SUBSCRIPTIONS,
                    stringResource(R.string.details_subscriptions),
                    Icons.Filled.Notifications,
                    onSubscriptionsSelected,
                ),
            )
        } else {
            // Гостю пункт виден с замком: тап объясняет, что даст вход.
            add(
                MobileDetailsAction(
                    DetailsButtonAction.SUBSCRIPTIONS,
                    stringResource(R.string.details_subscriptions),
                    Icons.Filled.Lock,
                    onSubscriptionsLockedSelected,
                ),
            )
        }
        add(
            MobileDetailsAction(
                DetailsButtonAction.SIMILAR,
                stringResource(R.string.details_similar),
                Icons.Filled.AutoAwesome,
                onSimilarSelected,
            ),
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.VIEWING_ORDER,
                stringResource(R.string.details_viewing_order),
                Icons.Filled.FormatListNumbered,
                onViewingOrderSelected,
            ),
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.COLLECTIONS,
                stringResource(R.string.details_collections_button),
                Icons.Filled.CollectionsBookmark,
                onCollectionsSelected,
            ),
        )
        add(
            MobileDetailsAction(
                DetailsButtonAction.REVIEWS,
                if (details.reviewsCount > 0) {
                    stringResource(
                        R.string.details_reviews_count,
                        details.reviewsCount,
                    )
                } else {
                    stringResource(R.string.details_reviews)
                },
                Icons.Filled.RateReview,
                onReviewsSelected,
            ),
        )
    }
    val byAction = availableActions.associateBy { it.action }
    return state.detailsButtonOrder
        .filterNot { it == DetailsButtonAction.WATCH || it == DetailsButtonAction.LIBRARY || it == DetailsButtonAction.FAVORITE }
        .mapNotNull { byAction[it] } +
        availableActions.filterNot { it.action in state.detailsButtonOrder }
}
