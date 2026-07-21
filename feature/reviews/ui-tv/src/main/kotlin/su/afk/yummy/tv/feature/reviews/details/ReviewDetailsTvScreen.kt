package su.afk.yummy.tv.feature.reviews.details

import android.widget.Toast
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.feature.reviews.model.ReviewContentBlock
import su.afk.yummy.tv.feature.reviews.tv.R
import su.afk.yummy.tv.feature.reviews.utils.parseReviewBlocks
import su.afk.yummy.tv.feature.reviews.utils.reviewStatusColor
import su.afk.yummy.tv.feature.reviews.utils.reviewStatusLabel
import su.afk.yummy.tv.feature.reviews.view.ReviewActionButton
import su.afk.yummy.tv.feature.reviews.view.ReviewReactionButton
import su.afk.yummy.tv.feature.reviews.view.ReviewRemoteImage
import su.afk.yummy.tv.feature.reviews.view.ReviewRichParagraph

@Composable
fun ReviewDetailsTvScreen(
    state: ReviewDetailsState.State,
    effect: Flow<ReviewDetailsState.Effect>,
    onEvent: (ReviewDetailsState.Event) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(effect) {
        effect.collect {
            if (it is ReviewDetailsState.Effect.ShowToast) {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    when {
        state.loading -> TvLoadingScreen()

        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TvStateMessage(
                title = stringResource(R.string.reviews_error),
                icon = Icons.Filled.Warning,
                onRetry = { onEvent(ReviewDetailsState.Event.RetrySelected) },
            )
        }

        state.details == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TvStateMessage(
                title = stringResource(R.string.reviews_empty),
                icon = Icons.Filled.RateReview,
            )
        }

        else -> {
            val details = requireNotNull(state.details)
            val blocks = remember(details.review.html) { parseReviewBlocks(details.review.html) }
            val topFocus = remember { FocusRequester() }
            LaunchedEffect(details.review.id) { runCatching { topFocus.requestFocus() } }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(TvScreenPadding.Horizontal),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = details.review.author.nickname,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(topFocus)
                            .tvFocusableClick(
                                onClick = {
                                    onEvent(ReviewDetailsState.Event.AuthorSelected(details.review.author.id))
                                },
                            ),
                    )
                }
                item {
                    Text(
                        details.review.status.reviewStatusLabel(),
                        color = details.review.status.reviewStatusColor(),
                        modifier = Modifier.focusable(),
                    )
                }
                details.review.rating?.let { rating ->
                    item {
                        Box(Modifier.focusable()) {
                            Column {
                                Text(
                                    "${rating.average ?: 0} / 10",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                rating.categories.forEach {
                                    Text(
                                        "${it.name}: ${it.score}/10",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                details.review.checkComment?.takeIf { it.isNotBlank() }?.let { comment ->
                    item {
                        Text(
                            comment,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.focusable(),
                        )
                    }
                }
                items(blocks, key = { it.id }) { block ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .focusable()
                    ) {
                        when (block) {
                            is ReviewContentBlock.Image -> ReviewRemoteImage(block.url, block.alt)
                            is ReviewContentBlock.Paragraph -> ReviewRichParagraph(block)
                        }
                    }
                }
                item {
                    val reactions = details.review.reactions
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReviewReactionButton(
                            icon = Icons.Filled.ThumbUp,
                            count = reactions.likes,
                            color = YummySemanticColors.Like,
                            selected = reactions.vote == ReviewVote.LIKE,
                            onClick = {
                                onEvent(
                                    ReviewDetailsState.Event.VoteSelected(
                                        if (reactions.vote == ReviewVote.LIKE) ReviewVote.NONE else ReviewVote.LIKE,
                                    ),
                                )
                            },
                        )
                        ReviewReactionButton(
                            icon = Icons.Filled.ThumbDown,
                            count = reactions.dislikes,
                            color = YummySemanticColors.Dislike,
                            selected = reactions.vote == ReviewVote.DISLIKE,
                            onClick = {
                                onEvent(
                                    ReviewDetailsState.Event.VoteSelected(
                                        if (reactions.vote == ReviewVote.DISLIKE) ReviewVote.NONE else ReviewVote.DISLIKE,
                                    ),
                                )
                            },
                        )
                    }
                }
                if (details.review.commentable) {
                    item {
                        ReviewActionButton(
                            label = stringResource(
                                R.string.review_comments_tv,
                                details.commentsCount.toCompactCount(),
                            ),
                            onClick = { onEvent(ReviewDetailsState.Event.CommentsSelected) },
                        )
                    }
                }
            }
        }
    }
}
