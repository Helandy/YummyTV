package su.afk.yummy.tv.feature.main.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.core.designsystem.presenter.components.GlobalToastOverlay
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.MainState
import su.afk.yummy.tv.feature.main.model.TvMenuItem

@Composable
fun TvMainScaffold(
    selectedRoot: RootTab,
    contentFocusKey: Any?,
    menuItems: List<TvMenuItem>,
    state: MainState.State,
    showMainMenu: Boolean = true,
    onEvent: (MainState.Event) -> Unit = {},
    toastMessage: String? = null,
    content: @Composable () -> Unit,
) {
    val focusController = rememberTvMainFocusController(showMainMenu)
    val selectedRootFocusRequester = focusController.selectedRootFocusRequester(selectedRoot)
    val currentPreferredContentFocusRequester =
        focusController.currentPreferredContentFocusRequester(selectedRoot, contentFocusKey)
    val registerPreferredContentFocusRequester = remember(
        focusController,
        contentFocusKey,
        selectedRoot,
    ) {
        focusController.preferredContentFocusRegistration(selectedRoot, contentFocusKey)
    }
    val accountLabel = if (state.isYaniSignedIn) state.yaniNickname else null
    val accountAvatarUrl = if (state.isYaniSignedIn) state.yaniAvatarUrl else ""
    var menuExpanded by remember { mutableStateOf(false) }
    val menuCanFocus = focusController.menuCanFocus &&
            focusController.previousShowMainMenu &&
            !focusController.restoreContentFocusAfterMenuShown

    LaunchedEffect(showMainMenu) {
        if (!showMainMenu) {
            menuExpanded = false
        }
    }

    val pendingContentFocus = focusController.pendingContentFocusRequest != null
    // pendingContentFocus покрывает окно nav-перехода: пока фокус контента не установлен,
    // BACK возвращает в меню вместо сворачивания приложения
    BackHandler(
        enabled = showMainMenu &&
                focusController.previousShowMainMenu &&
                !menuExpanded &&
                (pendingContentFocus || (menuCanFocus && focusController.contentHasFocus)),
    ) {
        focusController.cancelPendingContentFocusRequest()
        val focused = runCatching { selectedRootFocusRequester.requestFocus() }.getOrDefault(false)
        if (focused) {
            menuExpanded = true
        }
    }

    TvMainFocusEffects(
        focusController = focusController,
        showMainMenu = showMainMenu,
        selectedRoot = selectedRoot,
        contentFocusKey = contentFocusKey,
        currentPreferredContentFocusRequester = currentPreferredContentFocusRequester,
        selectedRootFocusRequester = selectedRootFocusRequester,
    )

    CompositionLocalProvider(
        LocalMainMenuFocusRequester provides selectedRootFocusRequester,
        LocalContentFocusRequester provides focusController.contentFocusRequester,
        LocalPreferredContentFocusRequester provides registerPreferredContentFocusRequester,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            TvMainContentPane(
                showMainMenu = showMainMenu,
                menuExpanded = menuExpanded,
                contentFocusRequester = focusController.contentFocusRequester,
                selectedRootFocusRequester = selectedRootFocusRequester,
                currentPreferredContentFocusRequester = currentPreferredContentFocusRequester,
                onFocusChanged = { isFocused, hasFocus ->
                    focusController.onContentFocusChanged(
                        isFocused = isFocused,
                        hasFocus = hasFocus,
                        currentPreferredContentFocusRequester = currentPreferredContentFocusRequester,
                    )
                },
            ) {
                content()
            }

            GlobalToastOverlay(text = toastMessage)

            if (showMainMenu) {
                TvSideMenu(
                    selectedRoot = selectedRoot,
                    menuItems = menuItems,
                    accountLabel = accountLabel,
                    accountAvatarUrl = accountAvatarUrl,
                    unreadNotificationsCount = state.unreadNotificationsCount,
                    expanded = menuExpanded,
                    onExpandedChange = { expanded ->
                        // Пока запрос фокуса контента в полёте, меню не должно
                        // само раскрываться, перехватив временно потерянный фокус
                        menuExpanded = expanded &&
                                focusController.pendingContentFocusRequest == null
                    },
                    onEvent = onEvent,
                    rootFocusRequesters = focusController.rootFocusRequesters,
                    menuEnterFocusRequester = selectedRootFocusRequester,
                    rightFocusRequesterForRoot = focusController.rightFocusRequesterForRoot,
                    canFocus = menuCanFocus,
                    onMenuNavigationFocusLocked = focusController::updateMenuNavigationFocusLocked,
                    onMoveToContent = { root ->
                        menuExpanded = false
                        focusController.requestContentFocus(root)
                    },
                )
            }
        }
    }
}
