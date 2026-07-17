package su.afk.yummy.tv.feature.bloggers.video

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.feature.bloggers.tv.R

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

/**
 * Текст описания, который при фокусе листает весь экран по D-pad: вверх/вниз крутят [scrollState],
 * пока есть куда, иначе фокус уходит дальше (без залипания).
 */
@Composable
private fun PageScrollableText(
    text: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val step = 240f
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> if (scrollState.canScrollForward) {
                        scope.launch { scrollState.animateScrollBy(step) }
                        true
                    } else false

                    Key.DirectionUp -> if (scrollState.canScrollBackward) {
                        scope.launch { scrollState.animateScrollBy(-step) }
                        true
                    } else false

                    else -> false
                }
            }
            .focusable(),
    )
}

/** Кнопка действия в стиле экрана деталей: видимый фокус (масштаб + рамка через [tvFocusableClick]). */
@Composable
private fun VideoActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
) {
    val shape = RoundedCornerShape(8.dp)
    val bgColor =
        if (filled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val contentColor =
        if (filled) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusableClick(onClick = onClick, shape = shape)
            .background(bgColor, shape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(label, color = contentColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Кнопка реакции (лайк/дизлайк) с цветным контентом и видимым фокусом. */
@Composable
private fun VideoReactionButton(
    icon: ImageVector,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val bgColor =
        if (selected) color.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .tvFocusableClick(onClick = onClick, shape = shape)
            .background(bgColor, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(count.toCompactCount(), color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}
