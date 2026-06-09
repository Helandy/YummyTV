package su.afk.yummy.tv.feature.main.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.feature.main.R
import su.afk.yummy.tv.feature.main.utils.moveFocusToContentOnKey

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvSideMenuAccountItem(
    label: String,
    signedIn: Boolean,
    avatarUrl: String,
    unreadNotificationsCount: Int,
    expanded: Boolean,
    selected: Boolean,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester,
    canFocus: Boolean,
    onFocused: () -> Unit,
    onMoveToContent: (force: Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val backgroundColor = when {
        focused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
        selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
        else -> Color.Transparent
    }
    val contentColor = when {
        focused -> MaterialTheme.colorScheme.surface
        selected -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .height(TvSideMenuItemHeight)
            .width(TvSideMenuExpandedWidth - 28.dp)
            .focusProperties {
                this.canFocus = canFocus
                downFocusRequester?.let { down = it }
                right = rightFocusRequester
            }
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clip(TvSideMenuShape)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .moveFocusToContentOnKey(onMoveToContent)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(28.dp)) {
            if (signedIn && avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(R.string.main_account_content_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.main_account_content_description),
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center),
                    tint = contentColor,
                )
            }
            if (signedIn && unreadNotificationsCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }
        if (expanded) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
