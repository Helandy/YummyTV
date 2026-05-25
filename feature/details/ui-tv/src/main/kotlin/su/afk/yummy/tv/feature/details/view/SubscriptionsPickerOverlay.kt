package su.afk.yummy.tv.feature.details.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.SubscriptionOption

@Composable
internal fun SubscriptionsPickerOverlay(
    subscriptions: List<SubscriptionOption>,
    isLoading: Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }
    val itemFocusRequesters = remember(subscriptions.size) { List(subscriptions.size) { FocusRequester() } }
    var focusedOptionIndex by remember { mutableIntStateOf(0) }
    var focusedOptionKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subscriptions.map { it.key }) {
        if (subscriptions.isNotEmpty() && focusedOptionKey == null) {
            withFrameNanos { }
            runCatching { firstFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(subscriptions.map { it.key }, focusedOptionKey) {
        val key = focusedOptionKey ?: return@LaunchedEffect
        val index = subscriptions.indexOfFirst { it.key == key }
        if (index >= 0) {
            focusedOptionIndex = index
            withFrameNanos { }
            runCatching { itemFocusRequesters.getOrNull(index)?.requestFocus() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .width(620.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF21B1B1F))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .focusGroup()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionUp -> focusedOptionIndex == 0
                            Key.DirectionDown -> focusedOptionIndex == subscriptions.lastIndex
                            else -> false
                        }
                    }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.details_subscriptions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
                when {
                    isLoading && subscriptions.isEmpty() -> SubscriptionMessage(stringResource(R.string.details_subscriptions_loading))
                    subscriptions.isEmpty() -> SubscriptionMessage(stringResource(R.string.details_subscriptions_empty))
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(subscriptions, key = { _, item -> item.key }) { index, option ->
                            SubscriptionOptionItem(
                                option = option,
                                focusRequester = if (index == 0) firstFocusRequester else itemFocusRequesters[index],
                                onFocused = {
                                    focusedOptionIndex = index
                                    focusedOptionKey = option.key
                                },
                                onClick = { onToggle(option.key) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.68f),
        modifier = Modifier.padding(8.dp),
    )
}

@Composable
private fun SubscriptionOptionItem(
    option: SubscriptionOption,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val bgColor by animateColorAsState(
        targetValue = if (focused) Color.White else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "subscription_bg",
    )
    val textColor by animateColorAsState(
        targetValue = if (focused) Color.Black else Color.White,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "subscription_text",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.details_subscription_dubbing, option.dubbing),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.details_subscription_player, option.player),
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.details_subscription_episodes, option.episodesCount),
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.62f),
                maxLines = 1,
            )
        }
        Text(
            text = stringResource(
                if (option.isSubscribed) R.string.details_subscription_unsubscribe
                else R.string.details_subscription_subscribe,
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
        )
    }
}
