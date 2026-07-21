package su.afk.yummy.tv.feature.bloggers.mobile.video

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileSectionLoading
import su.afk.yummy.tv.core.utils.formatFeedDateTime
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.feature.bloggers.mobile.R
import su.afk.yummy.tv.feature.bloggers.mobile.view.BloggerVideoCreatorCard
import su.afk.yummy.tv.feature.bloggers.mobile.view.BloggerVideoHero
import su.afk.yummy.tv.feature.bloggers.mobile.view.BloggerVideoReactions
import su.afk.yummy.tv.feature.bloggers.video.BloggerVideoDetailsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloggerVideoDetailsMobileScreen(
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
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    BackHandler { onEvent(BloggerVideoDetailsState.Event.BackSelected) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.blogger_video_details_title)) },
                navigationIcon = {
                    IconButton({ onEvent(BloggerVideoDetailsState.Event.BackSelected) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                onAction = { onEvent(BloggerVideoDetailsState.Event.RetrySelected) },
            )

            state.loading -> MobileSectionLoading(Modifier.padding(padding))

            video != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BloggerVideoHero(
                    video = video,
                    onWatch = { onEvent(BloggerVideoDetailsState.Event.WatchSelected) },
                )
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(
                            R.string.blogger_video_views,
                            video.views.toCompactCount(),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "• ${video.publishedAt.formatFeedDateTime()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BloggerVideoCreatorCard(
                    creator = video.creator,
                    onClick = { onEvent(BloggerVideoDetailsState.Event.BloggerSelected) },
                )
                BloggerVideoReactions(
                    reaction = video.reaction,
                    enabled = true,
                    onVote = { onEvent(BloggerVideoDetailsState.Event.VoteSelected(it)) },
                )
                FilledTonalButton(
                    onClick = { onEvent(BloggerVideoDetailsState.Event.CommentsSelected) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 13.dp),
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null)
                    Text(
                        text = stringResource(
                            R.string.blogger_video_comments,
                            video.commentsCount.toCompactCount(),
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (video.description.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.blogger_video_about),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = video.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
