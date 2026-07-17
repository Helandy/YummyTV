package su.afk.yummy.tv.feature.bloggers.details

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvRetryButton
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.feature.bloggers.tv.R
import su.afk.yummy.tv.feature.bloggers.view.BloggerVideoTvCard

@Composable
fun BloggerDetailsTvScreen(
    state: BloggerDetailsState.State,
    effect: Flow<BloggerDetailsState.Effect>,
    onEvent: (BloggerDetailsState.Event) -> Unit,
) {
    val context = LocalContext.current
    val blogger = state.blogger
    val error = state.error
    LaunchedEffect(effect) {
        effect.collect {
            if (it is BloggerDetailsState.Effect.ShowToast) Toast.makeText(
                context,
                it.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    BackHandler { onEvent(BloggerDetailsState.Event.BackSelected) }
    when {
        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TvStateMessage(
                title = error.ifBlank { stringResource(R.string.blogger_videos_error) },
                icon = Icons.Filled.Warning,
                onRetry = { onEvent(BloggerDetailsState.Event.RetrySelected) },
            )
        }

        state.loading -> TvLoadingScreen()

        blogger != null -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .tvFocusRestorer(),
            contentPadding = PaddingValues(
                horizontal = TvScreenPadding.Horizontal,
                vertical = TvScreenPadding.Vertical,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AsyncImage(
                        blogger.avatarUrl,
                        blogger.nickname,
                        Modifier
                            .size(150.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            blogger.nickname,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            stringResource(
                                R.string.blogger_subscribers_tv,
                                blogger.subscribers.toCompactCount(),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(
                                R.string.blogger_videos_count_tv,
                                blogger.videosCount.toCompactCount(),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TvRetryButton(
                            text = stringResource(
                                if (blogger.isSubscribed) R.string.blogger_unsubscribe
                                else R.string.blogger_subscribe,
                            ),
                            enabled = !state.subscribing,
                            onClick = { onEvent(BloggerDetailsState.Event.SubscribeSelected) },
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.blogger_videos_section),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (state.videos.isEmpty()) item {
                Text(
                    stringResource(R.string.blogger_videos_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.videos, key = { it.id }) { video ->
                BloggerVideoTvCard(
                    video,
                    { onEvent(BloggerDetailsState.Event.VideoSelected(video.id)) })
            }
        }
    }
}
