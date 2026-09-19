package su.afk.yummy.tv.feature.details.episodes.dubbings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.tv.TvStateMessage
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.utils.formatCompactCount
import su.afk.yummy.tv.feature.details.view.common.BalancerPickerOverlay

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun EpisodeDubbingsTvScreenDefaultPreview() = ScreenPreviewTheme {
    EpisodeDubbingsTvScreen(EpisodeDubbingsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
fun EpisodeDubbingsTvScreen(
    state: EpisodeDubbingsState.State,
    effect: Flow<EpisodeDubbingsState.Effect>,
    onEvent: (EpisodeDubbingsState.Event) -> Unit,
) {
    val balancerPicker = state.pendingBalancerSelection
    BackHandler(enabled = balancerPicker == null) {
        onEvent(EpisodeDubbingsState.Event.BackSelected)
    }

    var lastFocusedIndex by rememberSaveable(state.episode) { mutableIntStateOf(0) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var restoreFocusRequest by remember { mutableIntStateOf(0) }
    val focusRequesters =
        remember(state.dubbings) { List(state.dubbings.size) { FocusRequester() } }

    fun dismissBalancerPicker() {
        onEvent(EpisodeDubbingsState.Event.BalancerPickerDismissed)
        restoreFocusRequest += 1
    }

    LaunchedEffect(restoreFocusRequest) {
        if (restoreFocusRequest > 0) {
            isRestoringFocus = true
            focusRequesters.getOrNull(lastFocusedIndex)?.let { requestFocusUntilTimeout(it) }
            isRestoringFocus = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading -> TvLoadingScreen()

            state.error != null -> TvStateMessage(
                title = state.error.orEmpty(),
                onRetry = { onEvent(EpisodeDubbingsState.Event.RetrySelected) },
            )

            state.dubbings.isEmpty() -> TvStateMessage(
                title = stringResource(R.string.details_episode_dubbings_empty),
                icon = Icons.Filled.Mic,
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .tvFocusRestorer(
                        fallback = focusRequesters.getOrNull(lastFocusedIndex)
                            ?: FocusRequester.Default,
                    ),
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    top = TvScreenPadding.Vertical,
                    end = TvScreenPadding.Horizontal,
                    bottom = TvScreenPadding.Vertical,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = stringResource(
                            R.string.details_episode_dubbings_title,
                            state.episode,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                itemsIndexed(state.dubbings, key = { _, item -> item.name }) { index, dubbing ->
                    DubbingRow(
                        dubbing = dubbing,
                        modifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .onFocusChanged {
                                if (it.hasFocus && !isRestoringFocus) {
                                    lastFocusedIndex = index
                                }
                            },
                        onClick = {
                            lastFocusedIndex = index
                            onEvent(EpisodeDubbingsState.Event.DubbingSelected(dubbing.name))
                        },
                    )
                }
            }
        }

        if (balancerPicker != null) {
            BalancerPickerOverlay(
                picker = balancerPicker,
                onConfirmed = { option ->
                    onEvent(EpisodeDubbingsState.Event.BalancerConfirmed(option.video))
                },
                onDismiss = ::dismissBalancerPicker,
            )
        }
    }
}

@Composable
private fun DubbingRow(
    dubbing: EpisodeDubbingsState.DubbingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val contentColor =
        if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = dubbing.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                // Вне фокуса название — акцентным цветом, как в мобильных шторках выбора.
                color = if (focused) contentColor else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = dubbing.views.formatCompactCount(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                )
            }
            if (dubbing.supportedBalancers.isNotBlank()) {
                Text(
                    text = dubbing.supportedBalancers,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
