package su.afk.yummy.tv.feature.main.view

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalTopBarFocusRequester
import su.afk.yummy.tv.core.navigation.TopBarFocusTarget
import su.afk.yummy.tv.feature.main.R

data class TvMenuItem<T>(
    @param:StringRes val titleRes: Int,
    val destination: T,
    val icon: ImageVector? = null,
)

@Composable
fun <T> TvMainScaffold(
    selectedDestination: T,
    menuItems: List<TvMenuItem<T>>,
    onDestinationSelected: (T) -> Unit,
    onSettingsClick: () -> Unit = {},
    accountLabel: String? = null,
    onAccountClick: () -> Unit = {},
    showTopBar: Boolean = true,
    requestedTopBarFocusTarget: TopBarFocusTarget? = null,
    onTopBarFocusRequestHandled: (TopBarFocusTarget?) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val topBarFocusRequester = remember { FocusRequester() }
    val accountFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    var preferredContentFocusRequester by remember { mutableStateOf<FocusRequester?>(null) }
    var hasInitialisedFocus by remember { mutableStateOf(false) }
    var topBarHasFocus by remember { mutableStateOf(false) }
    var skipNextContentRedirect by remember { mutableStateOf(false) }
    val topBarDownFocusRequester = preferredContentFocusRequester ?: contentFocusRequester

    // The top bar is removed from composition while a nested screen (e.g. details) is open
    // and re-added on the way back. On the very first launch we hand focus to the top bar.
    // On every return-to-root, Compose's built-in focus recovery sometimes lands on the top
    // bar (settings) instead of the content. Watch for that for a short window after the
    // return and pull focus back into the content so the screen can restore its card. We only
    // act while focus is actually on the top bar, so a correct content focus is never disturbed.
    LaunchedEffect(showTopBar, requestedTopBarFocusTarget) {
        if (!showTopBar) return@LaunchedEffect
        when (requestedTopBarFocusTarget) {
            TopBarFocusTarget.TRAILING_ACTION -> {
                hasInitialisedFocus = true
                skipNextContentRedirect = true
                kotlinx.coroutines.delay(40)
                runCatching { accountFocusRequester.requestFocus() }
                onTopBarFocusRequestHandled(requestedTopBarFocusTarget)
                return@LaunchedEffect
            }
            TopBarFocusTarget.SELECTED_TAB -> {
                hasInitialisedFocus = true
                skipNextContentRedirect = true
                kotlinx.coroutines.delay(40)
                runCatching { topBarFocusRequester.requestFocus() }
                onTopBarFocusRequestHandled(requestedTopBarFocusTarget)
                return@LaunchedEffect
            }
            null -> Unit
        }

        if (!hasInitialisedFocus) {
            hasInitialisedFocus = true
            runCatching { topBarFocusRequester.requestFocus() }
        } else if (skipNextContentRedirect) {
            skipNextContentRedirect = false
        } else {
            var attempts = 0
            var redirected = false
            while (attempts < 40) {
                kotlinx.coroutines.delay(40)
                if (topBarHasFocus) {
                    runCatching { contentFocusRequester.requestFocus() }
                    redirected = true
                } else if (redirected) {
                    break
                }
                attempts++
            }
        }
    }

    CompositionLocalProvider(
        LocalTopBarFocusRequester provides topBarFocusRequester,
        LocalContentFocusRequester provides contentFocusRequester,
        LocalPreferredContentFocusRequester provides { preferredContentFocusRequester = it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (showTopBar) {
                TvTopBar(
                    selectedDestination = selectedDestination,
                    menuItems = menuItems,
                    onDestinationSelected = onDestinationSelected,
                    onSettingsClick = onSettingsClick,
                    accountLabel = accountLabel,
                    onAccountClick = onAccountClick,
                    selectedTabFocusRequester = topBarFocusRequester,
                    accountFocusRequester = accountFocusRequester,
                    downFocusRequester = topBarDownFocusRequester,
                    onFocusChanged = { topBarHasFocus = it },
                )
            }
            Box(
                modifier = Modifier
                    .focusRequester(contentFocusRequester)
                    .focusProperties { up = topBarFocusRequester }
                    .focusGroup(),
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun <T> TvTopBar(
    selectedDestination: T,
    menuItems: List<TvMenuItem<T>>,
    onDestinationSelected: (T) -> Unit,
    onSettingsClick: () -> Unit,
    accountLabel: String?,
    onAccountClick: () -> Unit,
    selectedTabFocusRequester: FocusRequester,
    accountFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
) {
    val initialFocusRequester = selectedTabFocusRequester

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .focusGroup()
            .focusProperties { onEnter = { selectedTabFocusRequester.requestFocus() } }
            .padding(horizontal = TvScreenPadding.Horizontal, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvSettingsButton(
            downFocusRequester = downFocusRequester,
            onClick = onSettingsClick,
        )

        Row(
            modifier = Modifier.weight(1f).focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            menuItems.forEach { item ->
                val isSelected = item.destination == selectedDestination
                TvNavTab(
                    label = stringResource(item.titleRes),
                    icon = item.icon,
                    selected = isSelected,
                    onSelected = { onDestinationSelected(item.destination) },
                    focusRequester = if (isSelected) initialFocusRequester else null,
                    downFocusRequester = downFocusRequester,
                )
            }
        }

        TvAccountButton(
            label = accountLabel?.ifBlank { null } ?: stringResource(R.string.main_account_sign_in),
            signedIn = !accountLabel.isNullOrBlank(),
            focusRequester = accountFocusRequester,
            downFocusRequester = downFocusRequester,
            onClick = onAccountClick,
        )
    }
}

@Composable
private fun TvNavTab(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onSelected: () -> Unit,
    focusRequester: FocusRequester?,
    downFocusRequester: FocusRequester,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val contentColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val indicatorColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    val focusRequesterModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = focusRequesterModifier
            .focusProperties {
                down = downFocusRequester
            }
            .onFocusChanged { if (it.isFocused) onSelected() }
            .background(
                color = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null) { onSelected() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(color = indicatorColor, shape = RoundedCornerShape(1.dp)),
        )
    }
}

@Composable
private fun TvSettingsButton(
    downFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(44.dp)
            .focusProperties {
                down = downFocusRequester
            }
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.main_settings_content_description),
            modifier = Modifier.size(24.dp),
            tint = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TvAccountButton(
    label: String,
    signedIn: Boolean,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(22.dp)
    val background = when {
        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        signedIn -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }
    val contentColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusProperties {
                down = downFocusRequester
            }
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .background(background, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = stringResource(R.string.main_account_content_description),
            modifier = Modifier.size(24.dp),
            tint = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
        )
    }
}
