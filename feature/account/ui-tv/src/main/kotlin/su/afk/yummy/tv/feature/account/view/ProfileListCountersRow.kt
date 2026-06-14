@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun ProfileListCountersRow(
    counts: UserProfileCounts,
    firstFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_watching),
            counts.watching,
            Color(0xFFFF6B6B)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_planned),
            counts.planned,
            Color(0xFFA678E8)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_completed),
            counts.completed,
            Color(0xFF69D38B)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_dropped),
            counts.dropped,
            Color(0xFF9CA3AF)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_postponed),
            counts.postponed,
            Color(0xFFFFC857)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_favorite),
            counts.favorite,
            Color(0xFFD86BFF)
        ),
    )
    val internalFocusRequesters = remember(items.size) {
        List(items.size) { FocusRequester() }
    }
    val focusRequesters = internalFocusRequesters.toMutableList().apply {
        if (isNotEmpty() && firstFocusRequester != null) {
            this[0] = firstFocusRequester
        }
    }
    var focusedIndex by remember(items.size) { mutableStateOf<Int?>(null) }

    FlowRow(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEachIndexed { index, item ->
            ProfileCounterChip(
                item = item,
                focused = focusedIndex == index,
                modifier = Modifier.profileCounterChipFocus(
                    index = index,
                    focusRequester = focusRequesters[index],
                    focusRequesters = focusRequesters,
                    upFocusRequester = upFocusRequester,
                    onFocused = { focusedIndex = index },
                    onUnfocused = {
                        if (focusedIndex == index) {
                            focusedIndex = null
                        }
                    },
                ),
            )
        }
    }
}

@Composable
private fun ProfileCounterChip(
    item: ProfileCounterItem,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val containerColor = if (focused) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }

    Row(
        modifier = modifier
            .widthIn(min = 132.dp, max = 180.dp)
            .graphicsLayer {
                val scale = if (focused) 1.02f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(containerColor)
            .border(
                width = if (focused) 3.dp else 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(item.color),
            )
        }
        Column {
            Text(
                text = item.count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Modifier.profileCounterChipFocus(
    index: Int,
    focusRequester: FocusRequester,
    focusRequesters: List<FocusRequester>,
    upFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onUnfocused: () -> Unit,
): Modifier {
    fun requestFocusAt(targetIndex: Int): Boolean {
        val requester = focusRequesters.getOrNull(targetIndex) ?: return false
        return runCatching { requester.requestFocus() }.isSuccess
    }

    val leftIndex = index - 1
    val rightIndex = index + 1

    return this
        .focusRequester(focusRequester)
        .focusProperties {
            if (leftIndex >= 0) {
                left = focusRequesters[leftIndex]
            }
            if (rightIndex < focusRequesters.size) {
                right = focusRequesters[rightIndex]
            }
            upFocusRequester?.let { up = it }
        }
        .onFocusChanged {
            if (it.isFocused) {
                onFocused()
            } else if (!it.hasFocus) {
                onUnfocused()
            }
        }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> {
                    if (leftIndex >= 0) {
                        requestFocusAt(leftIndex)
                    } else {
                        false
                    }
                }

                Key.DirectionRight -> {
                    if (rightIndex < focusRequesters.size) {
                        requestFocusAt(rightIndex)
                    } else {
                        false
                    }
                }

                Key.DirectionUp -> {
                    upFocusRequester?.let { runCatching { it.requestFocus() }.isSuccess } ?: false
                }

                Key.DirectionDown -> true
                else -> false
            }
        }
        .focusable()
}

private data class ProfileCounterItem(
    val label: String,
    val count: Int,
    val color: Color,
)
