package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import su.afk.yummy.tv.feature.schedule.R
import su.afk.yummy.tv.feature.schedule.model.ScheduleReleaseUi
import su.afk.yummy.tv.feature.schedule.utils.remainingText
import su.afk.yummy.tv.feature.schedule.utils.timeLabel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
internal fun ScheduleReleaseRow(
    release: ScheduleReleaseUi,
    now: ZonedDateTime,
    zone: ZoneId,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val item = release.item
    val releaseAt = Instant.ofEpochSecond(release.epochSeconds).atZone(zone)
    val aired = releaseAt.isAfter(now).not()
    val remainingLabels = scheduleRemainingLabels()
    val accentColor = if (aired) AiredColor else MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val active = focused || selected
    val shape = RoundedCornerShape(8.dp)
    val rowScale by animateFloatAsState(
        targetValue = if (focused) 1.025f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "schedule_row_scale",
    )
    val rowBackground = when {
        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
        aired -> AiredColor.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .zIndex(if (focused) 1f else 0f)
            .focusRequester(focusRequester)
            .focusProperties {
                leftFocusRequester?.let { left = it }
                upFocusRequester?.let { up = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                    return@onPreviewKeyEvent false
                }
                leftFocusRequester?.requestFocus() == true
            }
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = rowScale
                    scaleY = rowScale
                }
                .clip(shape)
                .background(rowBackground, shape)
                .border(
                    width = if (active) 2.dp else 0.dp,
                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = shape,
                ),
        )
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (aired) {
                        stringResource(R.string.schedule_episode_aired, release.episode)
                    } else {
                        stringResource(
                            R.string.schedule_episode_future,
                            release.episode,
                            releaseAt.remainingText(now, remainingLabels),
                        )
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = releaseAt.timeLabel(),
                modifier = Modifier.widthIn(min = 82.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
            )
        }
    }
}
