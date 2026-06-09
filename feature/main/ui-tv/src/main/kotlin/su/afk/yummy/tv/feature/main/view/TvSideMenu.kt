package su.afk.yummy.tv.feature.main.view

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.MainState
import su.afk.yummy.tv.feature.main.R
import su.afk.yummy.tv.feature.main.model.TvMenuItem

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TvSideMenu(
    selectedRoot: RootTab,
    menuItems: List<TvMenuItem>,
    accountLabel: String?,
    accountAvatarUrl: String,
    unreadNotificationsCount: Int,
    onEvent: (MainState.Event) -> Unit,
    rootFocusRequesters: Map<RootTab, FocusRequester>,
    menuEnterFocusRequester: FocusRequester,
    rightFocusRequesterForRoot: (RootTab) -> FocusRequester,
    canFocus: Boolean,
    onMenuNavigationFocusLocked: (Boolean) -> Unit,
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
    var menuFocusRestoreRoot by remember { mutableStateOf<RootTab?>(null) }
    var menuFocusRestoreToken by remember { mutableIntStateOf(0) }

    fun selectRootFromMenuFocus(root: RootTab) {
        onMenuNavigationFocusLocked(true)
        menuFocusRestoreRoot = root
        menuFocusRestoreToken += 1
        onEvent(MainState.Event.TvRootFocused(root))
    }

    fun enterRootContent(root: RootTab) {
        menuFocusRestoreRoot = null
        onMenuNavigationFocusLocked(false)
        if (selectedRoot != root) {
            onEvent(MainState.Event.TvRootFocused(root))
        }
        onMoveToContent(root)
    }

    val menuWidth by animateDpAsState(
        targetValue = if (hasFocus) TvSideMenuExpandedWidth else TvSideMenuCollapsedWidth,
        animationSpec = tween(durationMillis = 180),
        label = "TvSideMenuWidth",
    )
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(selectedRoot, menuFocusRestoreRoot, menuFocusRestoreToken) {
        val selectedIndex = focusedIndexFor(selectedRoot)
        focusedIndex = selectedIndex
        val shouldRestoreMenuFocus = menuFocusRestoreRoot == selectedRoot
        if (shouldRestoreMenuFocus || hasFocus) {
            repeat(if (shouldRestoreMenuFocus) 24 else 1) {
                withFrameNanos { }
                if (shouldRestoreMenuFocus && menuFocusRestoreRoot != selectedRoot) {
                    return@LaunchedEffect
                }
                runCatching { rowFocusRequesters[selectedIndex].requestFocus() }
            }
        }
    }

    Column(
        modifier = Modifier
            .width(menuWidth)
            .fillMaxHeight()
            .clipToBounds()
            .background(backgroundColor)
            .onFocusChanged {
                val gainedFocus = it.hasFocus && !hasFocus
                hasFocus = it.hasFocus
                if (gainedFocus) {
                    val selectedIndex = focusedIndexFor(selectedRoot)
                    focusedIndex = selectedIndex
                    runCatching { rowFocusRequesters[selectedIndex].requestFocus() }
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        val target = (focusedIndex - 1).coerceAtLeast(0)
                        if (target != focusedIndex) {
                            val targetRoot = rootForFocusedIndex(target)
                            focusedIndex = target
                            runCatching { rowFocusRequesters[target].requestFocus() }
                            selectRootFromMenuFocus(targetRoot)
                        }
                        true
                    }

                    Key.DirectionDown -> {
                        val target = (focusedIndex + 1).coerceAtMost(rowFocusRequesters.lastIndex)
                        if (target != focusedIndex) {
                            val targetRoot = rootForFocusedIndex(target)
                            focusedIndex = target
                            runCatching { rowFocusRequesters[target].requestFocus() }
                            selectRootFromMenuFocus(targetRoot)
                        }
                        true
                    }

                    Key.DirectionRight,
                    Key.DirectionCenter,
                    Key.Enter,
                    Key.NumPadEnter -> {
                        enterRootContent(rootForFocusedIndex(focusedIndex))
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
            rightFocusRequester = rightFocusRequesterForRoot(RootTab.ACCOUNT),
            canFocus = canFocus,
            onFocused = {
                focusedIndex = 0
                if (selectedRoot != RootTab.ACCOUNT) {
                    selectRootFromMenuFocus(RootTab.ACCOUNT)
                }
            },
            onMoveToContent = { _ ->
                enterRootContent(RootTab.ACCOUNT)
            },
            onClick = {
                enterRootContent(RootTab.ACCOUNT)
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
                    enterRootContent(item.destination)
                },
                focusRequester = rootFocusRequesters.getValue(item.destination),
                upFocusRequester = rowFocusRequesters.getOrNull(rowIndex - 1),
                downFocusRequester = rowFocusRequesters.getOrNull(rowIndex + 1),
                rightFocusRequester = rightFocusRequesterForRoot(item.destination),
                canFocus = canFocus,
                onFocused = {
                    focusedIndex = rowIndex
                    if (item.destination != selectedRoot) {
                        selectRootFromMenuFocus(item.destination)
                    }
                },
                onMoveToContent = { _ ->
                    enterRootContent(item.destination)
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
                enterRootContent(RootTab.SETTINGS)
            },
            focusRequester = rootFocusRequesters.getValue(RootTab.SETTINGS),
            upFocusRequester = rowFocusRequesters.getOrNull(rowFocusRequesters.lastIndex - 1),
            rightFocusRequester = rightFocusRequesterForRoot(RootTab.SETTINGS),
            canFocus = canFocus,
            onFocused = {
                focusedIndex = rowFocusRequesters.lastIndex
                if (selectedRoot != RootTab.SETTINGS) {
                    selectRootFromMenuFocus(RootTab.SETTINGS)
                }
            },
            onMoveToContent = { _ ->
                enterRootContent(RootTab.SETTINGS)
            },
        )
    }
}
