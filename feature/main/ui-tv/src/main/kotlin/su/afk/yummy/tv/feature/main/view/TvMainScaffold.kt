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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
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
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.MainState
import su.afk.yummy.tv.feature.main.R

private val SideMenuCollapsedWidth = 84.dp
private val SideMenuExpandedWidth = 300.dp
private val SideMenuItemHeight = 56.dp
private val SideMenuShape = RoundedCornerShape(8.dp)

private data class PendingContentFocusRequest(
    val root: RootTab,
    val requester: FocusRequester?,
)

data class TvMenuItem(
    @param:StringRes val titleRes: Int,
    val destination: RootTab,
    val icon: ImageVector? = null,
)

@Composable
fun TvMainScaffold(
    selectedRoot: RootTab,
    contentFocusKey: Any?,
    menuItems: List<TvMenuItem>,
    state: MainState.State,
    showMainMenu: Boolean = true,
    onEvent: (MainState.Event) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val rootFocusRequesters = remember {
        RootTab.entries.associateWith { FocusRequester() }
    }
    val contentFocusRequester = remember { FocusRequester() }
    var preferredContentFocusRequester by remember { mutableStateOf<FocusRequester?>(null) }
    var preferredContentFocusKey by remember { mutableStateOf<Any?>(null) }
    var initialContentFocusRequested by remember { mutableStateOf(false) }
    var pendingContentFocusRequest by remember { mutableStateOf<PendingContentFocusRequest?>(null) }
    val accountLabel = if (state.isYaniSignedIn) state.yaniNickname else null
    val accountAvatarUrl = if (state.isYaniSignedIn) state.yaniAvatarUrl else ""
    val currentPreferredContentFocusRequester =
        preferredContentFocusRequester.takeIf { preferredContentFocusKey == contentFocusKey }
    val mainMenuRightFocusRequester = currentPreferredContentFocusRequester ?: contentFocusRequester
    val selectedRootFocusRequester = rootFocusRequesters.getValue(selectedRoot)
    val registerPreferredContentFocusRequester = remember(contentFocusKey) {
        { requester: FocusRequester? ->
            if (requester != null) {
                preferredContentFocusKey = contentFocusKey
                preferredContentFocusRequester = requester
            } else if (preferredContentFocusKey == contentFocusKey) {
                preferredContentFocusRequester = null
                preferredContentFocusKey = null
            }
        }
    }
    val requestContentFocus: (RootTab) -> Unit = { root ->
        pendingContentFocusRequest = PendingContentFocusRequest(
            root = root,
            requester = currentPreferredContentFocusRequester.takeIf { root == selectedRoot },
        )
    }

    LaunchedEffect(showMainMenu, contentFocusKey, currentPreferredContentFocusRequester) {
        val initialRequester = currentPreferredContentFocusRequester
        if (!showMainMenu || initialContentFocusRequested || initialRequester == null) return@LaunchedEffect
        initialContentFocusRequested = true
        withFrameNanos { }
        withFrameNanos { }
        runCatching { initialRequester.requestFocus() }
    }

    LaunchedEffect(pendingContentFocusRequest, selectedRoot, contentFocusKey) {
        val request = pendingContentFocusRequest ?: return@LaunchedEffect
        if (request.root != selectedRoot) {
            withFrameNanos { }
            withFrameNanos { }
            if (request.root != selectedRoot) {
                pendingContentFocusRequest = null
            }
            return@LaunchedEffect
        }
        withFrameNanos { }
        withFrameNanos { }
        (request.requester ?: preferredContentFocusRequester
            .takeIf { preferredContentFocusKey == contentFocusKey })
            ?.let { runCatching { it.requestFocus() } }
        pendingContentFocusRequest = null
    }

    CompositionLocalProvider(
        LocalMainMenuFocusRequester provides selectedRootFocusRequester,
        LocalContentFocusRequester provides contentFocusRequester,
        LocalPreferredContentFocusRequester provides registerPreferredContentFocusRequester,
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
                    .focusProperties { left = selectedRootFocusRequester }
                    .moveUnhandledLeftFocusToMenu(
                        enabled = showMainMenu,
                        menuFocusRequester = selectedRootFocusRequester,
                    )
                    .focusGroup(),
            ) {
                content()
            }

            if (showMainMenu) {
                TvSideMenu(
                    selectedRoot = selectedRoot,
                    menuItems = menuItems,
                    accountLabel = accountLabel,
                    accountAvatarUrl = accountAvatarUrl,
                    unreadNotificationsCount = state.unreadNotificationsCount,
                    onEvent = onEvent,
                    rootFocusRequesters = rootFocusRequesters,
                    menuEnterFocusRequester = selectedRootFocusRequester,
                    rightFocusRequester = mainMenuRightFocusRequester,
                    onMoveToContent = requestContentFocus,
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TvSideMenu(
    selectedRoot: RootTab,
    menuItems: List<TvMenuItem>,
    accountLabel: String?,
    accountAvatarUrl: String,
    unreadNotificationsCount: Int,
    onEvent: (MainState.Event) -> Unit,
    rootFocusRequesters: Map<RootTab, FocusRequester>,
    menuEnterFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onMoveToContent: (RootTab) -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val rowFocusRequesters = listOf(rootFocusRequesters.getValue(RootTab.ACCOUNT)) +
            menuItems.map { rootFocusRequesters.getValue(it.destination) } +
            rootFocusRequesters.getValue(RootTab.SETTINGS)

    fun focusedIndexFor(root: RootTab): Int = when (root) {
        RootTab.ACCOUNT -> 0
        RootTab.SETTINGS -> rowFocusRequesters.lastIndex
        else -> (menuItems.indexOfFirst { it.destination == root } + 1)
            .coerceIn(1, rowFocusRequesters.lastIndex - 1)
    }

    var focusedIndex by remember { mutableIntStateOf(focusedIndexFor(selectedRoot)) }
    val menuWidth by animateDpAsState(
        targetValue = if (hasFocus) SideMenuExpandedWidth else SideMenuCollapsedWidth,
        animationSpec = tween(durationMillis = 180),
        label = "TvSideMenuWidth",
    )
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(selectedRoot, hasFocus) {
        if (!hasFocus) focusedIndex = focusedIndexFor(selectedRoot)
    }

    Column(
        modifier = Modifier
            .width(menuWidth)
            .fillMaxHeight()
            .clipToBounds()
            .background(backgroundColor)
            .onFocusChanged {
                hasFocus = it.hasFocus
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        val target = (focusedIndex - 1).coerceAtLeast(0)
                        if (target != focusedIndex) {
                            focusedIndex = target
                            runCatching { rowFocusRequesters[target].requestFocus() }
                        }
                        true
                    }

                    Key.DirectionDown -> {
                        val target = (focusedIndex + 1).coerceAtMost(rowFocusRequesters.lastIndex)
                        if (target != focusedIndex) {
                            focusedIndex = target
                            runCatching { rowFocusRequesters[target].requestFocus() }
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusGroup()
            .focusProperties { onEnter = { menuEnterFocusRequester.requestFocus() } }
            .padding(horizontal = 14.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvSideMenuAccountItem(
            label = accountLabel?.ifBlank { null } ?: stringResource(R.string.main_account_sign_in),
            signedIn = !accountLabel.isNullOrBlank(),
            avatarUrl = accountAvatarUrl,
            unreadNotificationsCount = unreadNotificationsCount,
            expanded = hasFocus,
            selected = selectedRoot == RootTab.ACCOUNT,
            focusRequester = rootFocusRequesters.getValue(RootTab.ACCOUNT),
            downFocusRequester = rowFocusRequesters.getOrNull(1),
            rightFocusRequester = rightFocusRequester,
            onFocusedSelected = {
                focusedIndex = 0
                onEvent(MainState.Event.TvRootFocused(RootTab.ACCOUNT))
            },
            onMoveToContent = { onMoveToContent(RootTab.ACCOUNT) },
            onClick = {
                onEvent(MainState.Event.TvRootFocused(RootTab.ACCOUNT))
                onMoveToContent(RootTab.ACCOUNT)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        menuItems.forEachIndexed { index, item ->
            val isSelected = item.destination == selectedRoot
            val rowIndex = index + 1
            TvSideMenuItem(
                label = stringResource(item.titleRes),
                icon = item.icon,
                selected = isSelected,
                expanded = hasFocus,
                onFocusedSelected = { onEvent(MainState.Event.TvRootFocused(item.destination)) },
                onActivated = {
                    onEvent(MainState.Event.TvRootFocused(item.destination))
                    onMoveToContent(item.destination)
                },
                focusRequester = rootFocusRequesters.getValue(item.destination),
                upFocusRequester = rowFocusRequesters.getOrNull(rowIndex - 1),
                downFocusRequester = rowFocusRequesters.getOrNull(rowIndex + 1),
                rightFocusRequester = rightFocusRequester,
                onFocused = { focusedIndex = rowIndex },
                onMoveToContent = { onMoveToContent(item.destination) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TvSideMenuItem(
            label = stringResource(R.string.main_settings_content_description),
            icon = Icons.Default.Settings,
            selected = selectedRoot == RootTab.SETTINGS,
            expanded = hasFocus,
            onFocusedSelected = { onEvent(MainState.Event.TvRootFocused(RootTab.SETTINGS)) },
            onActivated = {
                onEvent(MainState.Event.TvRootFocused(RootTab.SETTINGS))
                onMoveToContent(RootTab.SETTINGS)
            },
            focusRequester = rootFocusRequesters.getValue(RootTab.SETTINGS),
            upFocusRequester = rowFocusRequesters.getOrNull(rowFocusRequesters.lastIndex - 1),
            rightFocusRequester = rightFocusRequester,
            onFocused = { focusedIndex = rowFocusRequesters.lastIndex },
            onMoveToContent = { onMoveToContent(RootTab.SETTINGS) },
        )
    }
}

@Composable
private fun TvSideMenuItem(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    expanded: Boolean,
    onFocusedSelected: () -> Unit,
    onActivated: () -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester,
    onFocused: () -> Unit,
    onMoveToContent: () -> Unit,
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
            .height(SideMenuItemHeight)
            .width(SideMenuExpandedWidth - 28.dp)
            .moveFocusToContentOnKey(onMoveToContent)
            .focusProperties {
                right = rightFocusRequester
                upFocusRequester?.let { up = it }
                downFocusRequester?.let { down = it }
            }
            .onFocusChanged {
                if (it.isFocused) {
                    onFocused()
                    onFocusedSelected()
                }
            }
            .clip(SideMenuShape)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null) { onActivated() }
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
    selected: Boolean,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester,
    onFocusedSelected: () -> Unit,
    onMoveToContent: () -> Unit,
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
            .height(SideMenuItemHeight)
            .width(SideMenuExpandedWidth - 28.dp)
            .moveFocusToContentOnKey(onMoveToContent)
            .focusProperties {
                right = rightFocusRequester
                downFocusRequester?.let { down = it }
            }
            .onFocusChanged { if (it.isFocused) onFocusedSelected() }
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

private fun Modifier.moveFocusToContentOnKey(
    onMoveToContent: () -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionRight,
            Key.DirectionCenter,
            Key.Enter,
            Key.NumPadEnter,
                -> {
                onMoveToContent()
                true
            }

            else -> false
        }
    }

private fun Modifier.moveUnhandledLeftFocusToMenu(
    enabled: Boolean,
    menuFocusRequester: FocusRequester,
): Modifier =
    onKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
        if (event.key != Key.DirectionLeft) return@onKeyEvent false
        runCatching { menuFocusRequester.requestFocus() }
        true
    }
