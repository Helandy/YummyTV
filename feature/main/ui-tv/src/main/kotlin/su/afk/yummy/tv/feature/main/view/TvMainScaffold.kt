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
import kotlinx.coroutines.withTimeoutOrNull
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
private const val ContentFocusRestoreTimeoutMillis = 500L

private data class PendingContentFocusRequest(
    val root: RootTab,
    val requester: FocusRequester?,
    val token: Int,
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
    var contentFocusRequestToken by remember { mutableIntStateOf(0) }
    var previousShowMainMenu by remember { mutableStateOf(showMainMenu) }
    var restoreContentFocusAfterMenuShown by remember { mutableStateOf(false) }
    val menuCanFocus = !restoreContentFocusAfterMenuShown
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
        contentFocusRequestToken += 1
        pendingContentFocusRequest = PendingContentFocusRequest(
            root = root,
            requester = currentPreferredContentFocusRequester.takeIf { root == selectedRoot },
            token = contentFocusRequestToken,
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

    LaunchedEffect(showMainMenu) {
        if (showMainMenu && !previousShowMainMenu) {
            restoreContentFocusAfterMenuShown = true
        }
        previousShowMainMenu = showMainMenu
    }

    LaunchedEffect(
        showMainMenu,
        contentFocusKey,
        currentPreferredContentFocusRequester,
        restoreContentFocusAfterMenuShown
    ) {
        if (!showMainMenu || !restoreContentFocusAfterMenuShown) return@LaunchedEffect
        val requester = currentPreferredContentFocusRequester ?: contentFocusRequester
        requestFocusOnFrameBoundary(requester)
        restoreContentFocusAfterMenuShown = false
    }

    LaunchedEffect(
        pendingContentFocusRequest,
        selectedRoot,
        contentFocusKey,
        currentPreferredContentFocusRequester,
    ) {
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
        val requester = request.requester
            ?: currentPreferredContentFocusRequester
            ?: preferredContentFocusRequester.takeIf { preferredContentFocusKey == contentFocusKey }
            ?: return@LaunchedEffect
        repeat(4) {
            runCatching { requester.requestFocus() }
            withFrameNanos { }
        }
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
                    canFocus = menuCanFocus,
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
    canFocus: Boolean,
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
    fun rootForFocusedIndex(index: Int): RootTab = when (index) {
        0 -> RootTab.ACCOUNT
        rowFocusRequesters.lastIndex -> RootTab.SETTINGS
        else -> menuItems.getOrNull(index - 1)?.destination ?: selectedRoot
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
                            onEvent(MainState.Event.TvRootFocused(rootForFocusedIndex(target)))
                        }
                        true
                    }

                    Key.DirectionDown -> {
                        val target = (focusedIndex + 1).coerceAtMost(rowFocusRequesters.lastIndex)
                        if (target != focusedIndex) {
                            focusedIndex = target
                            runCatching { rowFocusRequesters[target].requestFocus() }
                            onEvent(MainState.Event.TvRootFocused(rootForFocusedIndex(target)))
                        }
                        true
                    }

                    Key.DirectionRight -> {
                        val targetRoot = rootForFocusedIndex(focusedIndex)
                        onEvent(MainState.Event.TvRootFocused(targetRoot))
                        onMoveToContent(targetRoot)
                        true
                    }

                    else -> false
                }
            }
            .focusGroup()
            .focusProperties {
                this.canFocus = canFocus
                onEnter = {
                    if (canFocus) {
                        menuEnterFocusRequester.requestFocus()
                    }
                }
            }
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
            canFocus = canFocus,
            onFocused = { focusedIndex = 0 },
            onMoveToContent = {
                onEvent(MainState.Event.TvRootFocused(RootTab.ACCOUNT))
                onMoveToContent(RootTab.ACCOUNT)
            },
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
                onActivated = {
                    onEvent(MainState.Event.TvRootFocused(item.destination))
                    onMoveToContent(item.destination)
                },
                focusRequester = rootFocusRequesters.getValue(item.destination),
                upFocusRequester = rowFocusRequesters.getOrNull(rowIndex - 1),
                downFocusRequester = rowFocusRequesters.getOrNull(rowIndex + 1),
                rightFocusRequester = rightFocusRequester,
                canFocus = canFocus,
                onFocused = { focusedIndex = rowIndex },
                onMoveToContent = {
                    onEvent(MainState.Event.TvRootFocused(item.destination))
                    onMoveToContent(item.destination)
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TvSideMenuItem(
            label = stringResource(R.string.main_settings_content_description),
            icon = Icons.Default.Settings,
            selected = selectedRoot == RootTab.SETTINGS,
            expanded = hasFocus,
            onActivated = {
                onEvent(MainState.Event.TvRootFocused(RootTab.SETTINGS))
                onMoveToContent(RootTab.SETTINGS)
            },
            focusRequester = rootFocusRequesters.getValue(RootTab.SETTINGS),
            upFocusRequester = rowFocusRequesters.getOrNull(rowFocusRequesters.lastIndex - 1),
            rightFocusRequester = rightFocusRequester,
            canFocus = canFocus,
            onFocused = { focusedIndex = rowFocusRequesters.lastIndex },
            onMoveToContent = {
                onEvent(MainState.Event.TvRootFocused(RootTab.SETTINGS))
                onMoveToContent(RootTab.SETTINGS)
            },
        )
    }
}

@Composable
private fun TvSideMenuItem(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    expanded: Boolean,
    onActivated: () -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester,
    canFocus: Boolean,
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
                this.canFocus = canFocus
                right = rightFocusRequester
                upFocusRequester?.let { up = it }
                downFocusRequester?.let { down = it }
            }
            .onFocusChanged {
                if (it.isFocused) {
                    onFocused()
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
    canFocus: Boolean,
    onFocused: () -> Unit,
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
                this.canFocus = canFocus
                right = rightFocusRequester
                downFocusRequester?.let { down = it }
            }
            .onFocusChanged { if (it.isFocused) onFocused() }
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

private suspend fun requestFocusOnFrameBoundary(
    requester: FocusRequester,
): Boolean =
    withTimeoutOrNull<Boolean>(ContentFocusRestoreTimeoutMillis) {
        repeat(2) { withFrameNanos { } }
        var focused = false
        while (!focused) {
            focused = runCatching { requester.requestFocus() }.getOrDefault(false)
            withFrameNanos { }
        }
        focused
    } ?: false
