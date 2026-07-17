package su.afk.yummy.tv.feature.bloggers.details

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileSectionLoading
import su.afk.yummy.tv.feature.bloggers.mobile.R
import su.afk.yummy.tv.feature.bloggers.view.BloggerVideoMobileCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloggerDetailsMobileScreen(
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        blogger?.nickname ?: stringResource(R.string.blogger_details_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(BloggerDetailsState.Event.BackSelected) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            error != null -> MobileMessage(
                title = error.ifBlank { stringResource(R.string.blogger_videos_error) },
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.blogger_videos_retry),
                onAction = { onEvent(BloggerDetailsState.Event.RetrySelected) },
            )

            state.loading -> MobileSectionLoading(Modifier.padding(padding))

            blogger != null -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AsyncImage(
                            model = blogger.avatarUrl,
                            contentDescription = blogger.nickname,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape),
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(blogger.nickname, style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.blogger_subscribers, blogger.subscribers))
                            Text(stringResource(R.string.blogger_videos_count, blogger.videosCount))
                        }
                    }
                }
                item {
                    if (blogger.isSubscribed) OutlinedButton(
                        onClick = { onEvent(BloggerDetailsState.Event.SubscribeSelected) },
                        enabled = !state.subscribing,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.blogger_unsubscribe)) }
                    else Button(
                        onClick = { onEvent(BloggerDetailsState.Event.SubscribeSelected) },
                        enabled = !state.subscribing,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.blogger_subscribe)) }
                }
                item {
                    Text(
                        stringResource(R.string.blogger_videos_section),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (state.videos.isEmpty()) item { Text(stringResource(R.string.blogger_videos_empty)) }
                items(state.videos, key = { it.id }) { video ->
                    BloggerVideoMobileCard(
                        video,
                        { onEvent(BloggerDetailsState.Event.VideoSelected(video.id)) })
                }
            }
        }
    }
}
