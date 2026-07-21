package su.afk.yummy.tv.feature.bloggers.video

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.feature.bloggers.tv.R
import su.afk.yummy.tv.feature.bloggers.video.view.PageScrollableText
import su.afk.yummy.tv.feature.bloggers.video.view.VideoActionButton
import su.afk.yummy.tv.feature.bloggers.video.view.VideoReactionButton

@Composable
fun BloggerVideoDetailsTvScreen(
    state: BloggerVideoDetailsState.State,
    effect: Flow<BloggerVideoDetailsState.Effect>,
    onEvent: (BloggerVideoDetailsState.Event) -> Unit,
) {
    val context = LocalContext.current
    val video = state.video
    val error = state.error
    LaunchedEffect(effect) {
        effect.collect {
            when (it) {
                is BloggerVideoDetailsState.Effect.OpenVideo -> context.openExternalUri(it.url)
                is BloggerVideoDetailsState.Effect.ShowToast -> Toast.makeText(
                    context,
                    it.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    BackHandler { onEvent(BloggerVideoDetailsState.Event.BackSelected) }
    when {
        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TvStateMessage(
                title = error.ifBlank { stringResource(R.string.blogger_videos_error) },
                icon = Icons.Filled.Warning,
                onRetry = { onEvent(BloggerVideoDetailsState.Event.RetrySelected) },
            )
        }

        state.loading -> TvLoadingScreen()

        video != null -> {
            val watchFocus = remember { FocusRequester() }
            LaunchedEffect(video.id) { runCatching { watchFocus.requestFocus() } }
            val pageScroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(pageScroll)
                    .padding(
                        horizontal = TvScreenPadding.Horizontal,
                        vertical = TvScreenPadding.Vertical,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Заголовок сверху, на всю ширину
                Text(
                    video.category.title,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    video.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    // Слева: превью
                    AsyncImage(
                        video.previewUrl,
                        video.title,
                        Modifier
                            .weight(1f)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    // Справа: действия, реакции, просмотры
                    Column(
                        Modifier.width(420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        VideoActionButton(
                            label = video.creator.nickname,
                            onClick = { onEvent(BloggerVideoDetailsState.Event.BloggerSelected) },
                        )
                        if (video.hasSpoiler) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    stringResource(R.string.blogger_video_spoiler),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        VideoActionButton(
                            label = stringResource(R.string.blogger_video_watch),
                            icon = Icons.Filled.PlayArrow,
                            filled = true,
                            onClick = { onEvent(BloggerVideoDetailsState.Event.WatchSelected) },
                            modifier = Modifier.focusRequester(watchFocus),
                        )
                        val commentsPhoneOnly = stringResource(R.string.blogger_comments_phone_only)
                        VideoActionButton(
                            label = stringResource(
                                R.string.blogger_video_comments,
                                video.commentsCount
                            ),
                            onClick = {
                                Toast.makeText(context, commentsPhoneOnly, Toast.LENGTH_SHORT)
                                    .show()
                            },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            VideoReactionButton(
                                icon = Icons.Filled.ThumbUp,
                                count = video.reaction.likes,
                                color = YummySemanticColors.Like,
                                selected = video.reaction.vote == BloggerVideoVote.LIKE,
                                onClick = {
                                    onEvent(
                                        BloggerVideoDetailsState.Event.VoteSelected(
                                            BloggerVideoVote.LIKE
                                        )
                                    )
                                },
                            )
                            VideoReactionButton(
                                icon = Icons.Filled.ThumbDown,
                                count = video.reaction.dislikes,
                                color = YummySemanticColors.Dislike,
                                selected = video.reaction.vote == BloggerVideoVote.DISLIKE,
                                onClick = {
                                    onEvent(
                                        BloggerVideoDetailsState.Event.VoteSelected(
                                            BloggerVideoVote.DISLIKE
                                        )
                                    )
                                },
                            )
                        }
                        Text(
                            stringResource(
                                R.string.blogger_video_views_tv,
                                video.views.toCompactCount()
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Описание на всю ширину под превью; листается вместе со всем экраном
                if (video.description.isNotBlank()) {
                    PageScrollableText(
                        text = video.description,
                        scrollState = pageScroll,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
