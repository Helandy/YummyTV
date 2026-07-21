package su.afk.yummy.tv.feature.reviews.mobile.details

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsState
import su.afk.yummy.tv.feature.reviews.mobile.R
import su.afk.yummy.tv.feature.reviews.mobile.utils.displayCompactReviewCount
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewAnimeCard
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewDetailsHeader
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewMobileRemoteImage
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewMobileRichParagraph
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewRatingCard
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewReactionsCard
import su.afk.yummy.tv.feature.reviews.model.ReviewContentBlock
import su.afk.yummy.tv.feature.reviews.utils.parseReviewBlocks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailsMobileScreen(
    state: ReviewDetailsState.State,
    effect: Flow<ReviewDetailsState.Effect>,
    onEvent: (ReviewDetailsState.Event) -> Unit,
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(effect) {
        effect.collect {
            if (it is ReviewDetailsState.Effect.ShowToast) {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.review_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onEvent(ReviewDetailsState.Event.DeleteConfirmed)
                }) { Text(stringResource(R.string.review_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.review_cancel))
                }
            },
        )
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.review_details_title),
                onBack = { onEvent(ReviewDetailsState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.loading,
            error = state.error?.let { stringResource(R.string.reviews_error) },
            onRetry = { onEvent(ReviewDetailsState.Event.RetrySelected) },
            empty = state.details == null,
            emptyText = stringResource(R.string.reviews_empty),
        ) {
            val details = requireNotNull(state.details)
            val review = details.review
            val blocks = remember(review.html) { parseReviewBlocks(review.html) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ReviewDetailsHeader(
                        review = review,
                        onAuthorClick = {
                            onEvent(ReviewDetailsState.Event.AuthorSelected(review.author.id))
                        },
                    )
                }
                item {
                    ReviewAnimeCard(
                        title = details.animeTitle.ifBlank { review.animeTitle },
                        posterUrl = details.animePosterUrl ?: review.animePosterUrl,
                        onClick = {
                            onEvent(ReviewDetailsState.Event.AnimeSelected(review.animeId))
                        },
                    )
                }
                review.rating?.let { rating ->
                    item { ReviewRatingCard(rating) }
                }
                review.checkComment?.takeIf { it.isNotBlank() }?.let { comment ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(comment, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
                items(blocks, key = { it.id }) { block ->
                    when (block) {
                        is ReviewContentBlock.Image -> ReviewMobileRemoteImage(block.url, block.alt)
                        is ReviewContentBlock.Paragraph -> ReviewMobileRichParagraph(block)
                    }
                }
                item {
                    ReviewReactionsCard(
                        reactions = review.reactions,
                        onVote = { onEvent(ReviewDetailsState.Event.VoteSelected(it)) },
                    )
                }
                if (review.commentable) {
                    item {
                        FilledTonalButton(
                            onClick = { onEvent(ReviewDetailsState.Event.CommentsSelected) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 13.dp),
                        ) {
                            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null)
                            Text(
                                text = stringResource(
                                    R.string.review_comments,
                                    details.commentsCount.displayCompactReviewCount(),
                                ),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                if (state.isOwner) {
                    item {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            enabled = !state.deleting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.review_delete))
                        }
                    }
                }
            }
        }
    }
}
