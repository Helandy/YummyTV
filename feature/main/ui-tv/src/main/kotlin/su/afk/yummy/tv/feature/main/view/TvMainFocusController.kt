package su.afk.yummy.tv.feature.main.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.model.PendingContentFocusRequest
import su.afk.yummy.tv.feature.main.model.RegisteredContentFocusRequester
import su.afk.yummy.tv.feature.main.utils.isContentFocusKeyFor
import su.afk.yummy.tv.feature.main.utils.requestFocusOnFrameBoundary

private const val FOCUS_RESTORE_ATTEMPTS = 3

internal class TvMainFocusController(
    initialShowMainMenu: Boolean,
) {
    val rootFocusRequesters: Map<RootTab, FocusRequester> =
        RootTab.entries.associateWith { FocusRequester() }
    val contentFocusRequester = FocusRequester()
    val rightFocusRequesterForRoot: (RootTab) -> FocusRequester = { FocusRequester.Cancel }

    private val preferredContentFocusRequesters =
        mutableStateMapOf<RootTab, RegisteredContentFocusRequester>()

    var initialContentFocusRequested by mutableStateOf(false)
        private set
    var isContentFocusInitialized by mutableStateOf(false)
        private set
    var pendingContentFocusRequest by mutableStateOf<PendingContentFocusRequest?>(null)
        private set
    private var contentFocusRequestToken by mutableIntStateOf(0)
    var previousShowMainMenu by mutableStateOf(initialShowMainMenu)
    var restoreContentFocusAfterMenuShown by mutableStateOf(false)
        private set
    var menuNavigationFocusLocked by mutableStateOf(false)
        private set
    var menuFocusRestoreAfterContentStealToken by mutableIntStateOf(0)
        private set

    val menuCanFocus: Boolean
        get() = isContentFocusInitialized

    fun selectedRootFocusRequester(selectedRoot: RootTab): FocusRequester =
        rootFocusRequesters.getValue(selectedRoot)

    fun currentPreferredContentFocusRequester(
        selectedRoot: RootTab,
        contentFocusKey: Any?,
    ): FocusRequester? =
        preferredContentFocusRequesters[selectedRoot]?.takeIf {
            it.key == contentFocusKey && it.key.isContentFocusKeyFor(selectedRoot)
        }?.requester

    fun preferredContentFocusRegistration(
        selectedRoot: RootTab,
        contentFocusKey: Any?,
    ): (FocusRequester?) -> Unit {
        val root = selectedRoot
        val key = contentFocusKey
        return { requester ->
            if (requester != null) {
                preferredContentFocusRequesters[root] = RegisteredContentFocusRequester(
                    key = key,
                    requester = requester,
                )
            } else {
                val current = preferredContentFocusRequesters[root]
                if (current?.key == key) {
                    preferredContentFocusRequesters.remove(root)
                }
            }
        }
    }

    fun requestContentFocus(root: RootTab) {
        menuNavigationFocusLocked = false
        contentFocusRequestToken += 1
        pendingContentFocusRequest = PendingContentFocusRequest(
            root = root,
            token = contentFocusRequestToken,
        )
    }

    fun updateMenuNavigationFocusLocked(locked: Boolean) {
        menuNavigationFocusLocked = locked
    }

    fun onContentFocusChanged(
        isFocused: Boolean,
        hasFocus: Boolean,
        currentPreferredContentFocusRequester: FocusRequester?,
    ) {
        if (hasFocus) {
            isContentFocusInitialized = true
        }
        if (hasFocus && menuNavigationFocusLocked) {
            menuFocusRestoreAfterContentStealToken += 1
        }
    }

    fun onInitialContentFocusRequested(focused: Boolean) {
        initialContentFocusRequested = true
        isContentFocusInitialized = focused
    }

    fun onMainMenuShownAgain() {
        restoreContentFocusAfterMenuShown = true
        isContentFocusInitialized = false
        menuNavigationFocusLocked = false
    }

    fun onContentFocusRestoredAfterMenuShown() {
        restoreContentFocusAfterMenuShown = false
        isContentFocusInitialized = true
    }

    fun clearPendingContentFocusRequest(token: Int) {
        if (pendingContentFocusRequest?.token == token) {
            pendingContentFocusRequest = null
        }
    }
}

@Composable
internal fun rememberTvMainFocusController(showMainMenu: Boolean): TvMainFocusController =
    remember { TvMainFocusController(initialShowMainMenu = showMainMenu) }

@Composable
internal fun TvMainFocusEffects(
    focusController: TvMainFocusController,
    showMainMenu: Boolean,
    selectedRoot: RootTab,
    contentFocusKey: Any?,
    currentPreferredContentFocusRequester: FocusRequester?,
    selectedRootFocusRequester: FocusRequester,
) {
    LaunchedEffect(
        showMainMenu,
        contentFocusKey,
        currentPreferredContentFocusRequester,
    ) {
        if (focusController.initialContentFocusRequested) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        withFrameNanos { }
        val focused = runCatching {
            focusController.contentFocusRequester.requestFocus()
        }.getOrDefault(false)
        focusController.onInitialContentFocusRequested(focused)
    }

    LaunchedEffect(
        focusController.menuFocusRestoreAfterContentStealToken,
        focusController.menuNavigationFocusLocked,
        selectedRootFocusRequester,
    ) {
        if (
            focusController.menuFocusRestoreAfterContentStealToken == 0 ||
            !focusController.menuNavigationFocusLocked
        ) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        runCatching { selectedRootFocusRequester.requestFocus() }
    }

    LaunchedEffect(showMainMenu) {
        if (showMainMenu && !focusController.previousShowMainMenu) {
            focusController.onMainMenuShownAgain()
        }
        focusController.previousShowMainMenu = showMainMenu
    }

    LaunchedEffect(
        showMainMenu,
        contentFocusKey,
        currentPreferredContentFocusRequester,
        focusController.restoreContentFocusAfterMenuShown,
    ) {
        if (!showMainMenu || !focusController.restoreContentFocusAfterMenuShown) return@LaunchedEffect
        var restored = false
        repeat(FOCUS_RESTORE_ATTEMPTS) {
            restored = requestFocusOnFrameBoundary(focusController.contentFocusRequester)
            if (restored) return@repeat
            withFrameNanos { }
        }
        if (restored) {
            focusController.onContentFocusRestoredAfterMenuShown()
        }
    }

    LaunchedEffect(
        focusController.pendingContentFocusRequest,
        selectedRoot,
        contentFocusKey,
        currentPreferredContentFocusRequester,
    ) {
        val request = focusController.pendingContentFocusRequest ?: return@LaunchedEffect
        if (request.root != selectedRoot || !contentFocusKey.isContentFocusKeyFor(selectedRoot)) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        withFrameNanos { }
        var restored = false
        repeat(FOCUS_RESTORE_ATTEMPTS) {
            restored = requestFocusOnFrameBoundary(focusController.contentFocusRequester)
            if (restored) return@repeat
            withFrameNanos { }
        }
        if (restored) {
            focusController.clearPendingContentFocusRequest(request.token)
        }
    }
}
