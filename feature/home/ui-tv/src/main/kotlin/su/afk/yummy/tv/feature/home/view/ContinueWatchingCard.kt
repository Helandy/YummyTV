package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.feature.home.R
import su.afk.yummy.tv.feature.home.utils.isLikelyImageUrl
import su.afk.yummy.tv.feature.home.utils.msToTimeString
import su.afk.yummy.tv.feature.home.utils.resolveEpisodeThumbnail

private val CardWidth = 220.dp
private val ThumbnailHeight = 124.dp
private val InProgressColor = Color(0xFF4CAF50)

@Composable
internal fun ContinueWatchingCard(
    entry: WatchProgressEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val progress = if (entry.durationMs > 0) entry.positionMs.toFloat() / entry.durationMs else 0f
    val positionLabel = entry.positionMs.msToTimeString()
    val durationLabel = entry.durationMs.msToTimeString()

    val kodikThumbnail by produceState<String?>(null, entry.screenshotUrl, entry.episodeUrl) {
        value = resolveEpisodeThumbnail(entry)
    }
    val directScreenshotUrl = entry.screenshotUrl.takeIf { it.isLikelyImageUrl() }
    val imageUrl = kodikThumbnail
        ?: directScreenshotUrl
        ?: entry.posterUrl.ifBlank { null }

    val shape = RoundedCornerShape(8.dp)

    Column(modifier = modifier.width(CardWidth)) {
        Text(
            text = entry.animeTitle,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties {
                    upFocusRequester?.let { up = it }
                    downFocusRequester?.let { down = it }
                }
                .onFocusChanged { state ->
                    val focused = state.isFocused || state.hasFocus
                    if (focused && !isFocused) onFocused()
                    isFocused = focused
                }
                .tvFocusableClick(onClick = onClick, shape = shape),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ThumbnailHeight)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(InProgressColor.copy(alpha = 0.25f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(3.dp)
                                .background(InProgressColor),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val episodeLabel = if (entry.episode.isNotBlank()) {
                        stringResource(R.string.home_episode_number, entry.episode)
                    } else {
                        stringResource(R.string.home_episode)
                    }
                    Text(
                        text = episodeLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$positionLabel / $durationLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
