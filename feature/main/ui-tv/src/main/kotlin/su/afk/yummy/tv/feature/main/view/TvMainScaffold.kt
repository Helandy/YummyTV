package su.afk.yummy.tv.feature.main.view

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.navigation.MainMenuFocusTarget
import su.afk.yummy.tv.feature.main.R

private val SideMenuCollapsedWidth = 84.dp
private val SideMenuExpandedWidth = 300.dp
private val SideMenuItemHeight = 56.dp
private val SideMenuShape = RoundedCornerShape(8.dp)

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
    accountAvatarUrl: String = "",
    unreadNotificationsCount: Int = 0,
    onAccountClick: () -> Unit = {},
    showMainMenu: Boolean = true,
    requestedMainMenuFocusTarget: MainMenuFocusTarget? = null,
    onMainMenuFocusRequestHandled: (MainMenuFocusTarget?) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val mainMenuFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val accountFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    var preferredContentFocusRequester by remember { mutableStateOf<FocusRequester?>(null) }
    var hasInitialisedFocus by remember { mutableStateOf(false) }
    var mainMenuHasFocus by remember { mutableStateOf(false) }
    var skipNextContentRedirect by remember { mutableStateOf(false) }
    val mainMenuRightFocusRequester = preferredContentFocusRequester ?: contentFocusRequester

    LaunchedEffect(showMainMenu, requestedMainMenuFocusTarget) {
        if (!showMainMenu) return@LaunchedEffect
        when (requestedMainMenuFocusTarget) {
            MainMenuFocusTarget.SETTINGS_ACTION -> {
                hasInitialisedFocus = true
                skipNextContentRedirect = true
                kotlinx.coroutines.delay(40)
                runCatching { settingsFocusRequester.requestFocus() }
                onMainMenuFocusRequestHandled(requestedMainMenuFocusTarget)
                return@LaunchedEffect
            }
            MainMenuFocusTarget.ACCOUNT_ACTION -> {
                hasInitialisedFocus = true
                skipNextContentRedirect = true
                kotlinx.coroutines.delay(40)
                runCatching { accountFocusRequester.requestFocus() }
                onMainMenuFocusRequestHandled(requestedMainMenuFocusTarget)
                return@LaunchedEffect
            }
            MainMenuFocusTarget.SELECTED_TAB -> {
                hasInitialisedFocus = true
                skipNextContentRedirect = true
                kotlinx.coroutines.delay(40)
                runCatching { mainMenuFocusRequester.requestFocus() }
                onMainMenuFocusRequestHandled(requestedMainMenuFocusTarget)
                return@LaunchedEffect
            }
            null -> Unit
        }

        if (!hasInitialisedFocus) {
            hasInitialisedFocus = true
            runCatching { mainMenuRightFocusRequester.requestFocus() }
        } else if (skipNextContentRedirect) {
            skipNextContentRedirect = false
        } else {
            runCatching { contentFocusRequester.requestFocus() }
            var attempts = 0
            var redirected = false
            while (attempts < 40) {
                kotlinx.coroutines.delay(20)
                if (mainMenuHasFocus) {
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
        LocalMainMenuFocusRequester provides mainMenuFocusRequester,
        LocalContentFocusRequester provides contentFocusRequester,
        LocalPreferredContentFocusRequester provides { preferredContentFocusRequester = it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = if (showMainMenu) SideMenuCollapsedWidth else 0.dp)
                    .focusRequester(contentFocusRequester)
                    .focusProperties { left = mainMenuFocusRequester }
                    .focusGroup(),
            ) {
                content()
            }

            if (showMainMenu) {
                TvSideMenu(
                    selectedDestination = selectedDestination,
                    menuItems = menuItems,
                    onDestinationSelected = onDestinationSelected,
                    onSettingsClick = onSettingsClick,
                    accountLabel = accountLabel,
                    accountAvatarUrl = accountAvatarUrl,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onAccountClick = onAccountClick,
                    settingsFocusRequester = settingsFocusRequester,
                    selectedTabFocusRequester = mainMenuFocusRequester,
                    accountFocusRequester = accountFocusRequester,
                    rightFocusRequester = mainMenuRightFocusRequester,
                    onFocusChanged = { mainMenuHasFocus = it },
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun <T> TvSideMenu(
    selectedDestination: T,
    menuItems: List<TvMenuItem<T>>,
    onDestinationSelected: (T) -> Unit,
    onSettingsClick: () -> Unit,
    accountLabel: String?,
    accountAvatarUrl: String,
    unreadNotificationsCount: Int,
    onAccountClick: () -> Unit,
    settingsFocusRequester: FocusRequester,
    selectedTabFocusRequester: FocusRequester,
    accountFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val menuWidth by animateDpAsState(
        targetValue = if (hasFocus) SideMenuExpandedWidth else SideMenuCollapsedWidth,
        animationSpec = tween(durationMillis = 180),
        label = "TvSideMenuWidth",
    )
    val backgroundColor = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .width(menuWidth)
            .fillMaxHeight()
            .clipToBounds()
            .background(backgroundColor)
            .onFocusChanged {
                hasFocus = it.hasFocus
                onFocusChanged(it.hasFocus)
            }
            .focusGroup()
            .focusProperties { onEnter = { selectedTabFocusRequester.requestFocus() } }
            .padding(horizontal = 14.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvSideMenuAccountItem(
            label = accountLabel?.ifBlank { null } ?: stringResource(R.string.main_account_sign_in),
            signedIn = !accountLabel.isNullOrBlank(),
            avatarUrl = accountAvatarUrl,
            unreadNotificationsCount = unreadNotificationsCount,
            expanded = hasFocus,
            focusRequester = accountFocusRequester,
            rightFocusRequester = rightFocusRequester,
            onClick = onAccountClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        menuItems.forEach { item ->
            val isSelected = item.destination == selectedDestination
            TvSideMenuItem(
                label = stringResource(item.titleRes),
                icon = item.icon,
                selected = isSelected,
                expanded = hasFocus,
                onSelected = { onDestinationSelected(item.destination) },
                focusRequester = if (isSelected) selectedTabFocusRequester else null,
                rightFocusRequester = rightFocusRequester,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TvSideMenuItem(
            label = stringResource(R.string.main_settings_content_description),
            icon = Icons.Default.Settings,
            selected = false,
            expanded = hasFocus,
            onSelected = onSettingsClick,
            focusRequester = settingsFocusRequester,
            rightFocusRequester = rightFocusRequester,
            selectOnFocus = false,
        )
    }
}

@Composable
private fun TvSideMenuItem(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    expanded: Boolean,
    onSelected: () -> Unit,
    focusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester,
    selectOnFocus: Boolean = true,
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
    val focusRequesterModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

    Row(
        modifier = focusRequesterModifier
            .height(SideMenuItemHeight)
            .width(SideMenuExpandedWidth - 28.dp)
            .moveFocusRightOnKey(rightFocusRequester)
            .focusProperties { right = rightFocusRequester }
            .onFocusChanged { if (selectOnFocus && it.isFocused) onSelected() }
            .clip(SideMenuShape)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null) { onSelected() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor,
                )
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

@Composable
private fun TvSideMenuAccountItem(
    label: String,
    signedIn: Boolean,
    avatarUrl: String,
    unreadNotificationsCount: Int,
    expanded: Boolean,
    focusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val backgroundColor = when {
        focused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
        signedIn -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val contentColor = if (focused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .height(SideMenuItemHeight)
            .width(SideMenuExpandedWidth - 28.dp)
            .moveFocusRightOnKey(rightFocusRequester)
            .focusProperties { right = rightFocusRequester }
            .clip(SideMenuShape)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
                    modifier = Modifier.size(28.dp).align(Alignment.Center),
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
                fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

private fun Modifier.moveFocusRightOnKey(focusRequester: FocusRequester): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
            runCatching { focusRequester.requestFocus(FocusDirection.Right) }
            true
        } else {
            false
        }
    }
