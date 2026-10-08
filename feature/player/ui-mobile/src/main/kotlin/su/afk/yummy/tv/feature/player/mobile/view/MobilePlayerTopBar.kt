package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.feature.player.mobile.R
import su.afk.yummy.tv.feature.player.mobile.utils.labelRes

@Composable
internal fun MobilePlayerTopBar(
    title: String,
    episode: String,
    dubbing: String,
    playerName: String,
    onBack: () -> Unit,
    onDetails: () -> Unit,
    onPictureInPicture: () -> Unit,
    orientationMode: PlayerOrientationMode,
    onOrientationSelected: (PlayerOrientationMode) -> Unit,
    showDetails: Boolean,
    showPictureInPicture: Boolean,
    showCast: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    var showActionsSheet by remember { mutableStateOf(false) }
    var showOrientationSheet by remember { mutableStateOf(false) }

    if (visible) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.28f))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (episode.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.player_mobile_episode_number, episode),
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (dubbing.isNotBlank()) {
                        Text(
                            text = dubbing,
                            color = Color.White.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    if (playerName.isNotBlank()) {
                        Text(
                            text = playerName.uppercase(),
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.88f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                    IconButton(
                        onClick = { showActionsSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .padding(top = 2.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.player_mobile_actions),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }

    if (showActionsSheet) {
        PlayerMobileActionsSheet(
            showDetails = showDetails,
            showPictureInPicture = showPictureInPicture,
            showCast = showCast,
            orientationLabel = stringResource(orientationMode.labelRes),
            onDetails = onDetails,
            onOrientation = { showOrientationSheet = true },
            onPictureInPicture = onPictureInPicture,
            onDismiss = { showActionsSheet = false },
        )
    }

    if (showOrientationSheet) {
        MobilePlayerOrientationSheet(
            selected = orientationMode,
            onSelected = { mode ->
                showOrientationSheet = false
                onOrientationSelected(mode)
            },
            onDismiss = { showOrientationSheet = false },
        )
    }
}
