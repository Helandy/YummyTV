package su.afk.yummy.tv.feature.library

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.components.ErrorBanner
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.feature.library.model.LibraryRemoveTarget
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.model.PendingLibraryTvRemoval
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.utils.focusStateKey
import su.afk.yummy.tv.feature.library.utils.isLocalLoading
import su.afk.yummy.tv.feature.library.utils.label
import su.afk.yummy.tv.feature.library.utils.tvTabItemCount
import su.afk.yummy.tv.feature.library.view.ContinueWatchingGrid
import su.afk.yummy.tv.feature.library.view.LibraryGrid
import su.afk.yummy.tv.feature.library.view.LibraryTopTabs
import su.afk.yummy.tv.feature.library.view.LibraryTvRemoveConfirmDialog

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
@Composable
private fun LibraryTvScreenDefaultPreview() = ScreenPreviewTheme {
    LibraryTvScreen(LibraryState.State(), emptyFlow()) {}
}

@Composable
fun LibraryTvScreen(
    state: LibraryState.State,
    effect: Flow<LibraryState.Effect>,
    onEvent: (LibraryState.Event) -> Unit,
) {
    // История на ТВ — отдельный пункт бокового меню: во вкладках её нет, чтобы LEFT с первой
    // вкладки уходил в меню, а не на «Историю».
    val tvTabs = remember(state.tabs) { state.tabs.filterNot { it == LibraryTab.HISTORY } }
    val selectedTab = state.selectedTab.takeIf { it != LibraryTab.HISTORY }
        ?: tvTabs.firstOrNull()
        ?: LibraryTab.CONTINUE_WATCHING
    LaunchedEffect(state.selectedTab, selectedTab) {
        if (state.selectedTab != selectedTab) onEvent(LibraryState.Event.TabSelected(selectedTab))
    }
    val gridFocusRequester = remember { FocusRequester() }
    // Вкладок фиксированный набор: запросы фокуса не зависят от порядка, который задаёт пользователь.
    val tabFocusRequesters = remember {
        LibraryTab.entries.associateWith { FocusRequester() }
    }
    val selectedTabFocusRequester = tabFocusRequesters.getValue(selectedTab)
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val itemRemovedText = stringResource(R.string.library_remove_success)
    val scope = rememberCoroutineScope()
    val currentOnEvent by rememberUpdatedState(onEvent)
    var restoreGridFocusOnResume by rememberSaveable { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<PendingLibraryTvRemoval?>(null) }
    val selectedTabTitle = selectedTab.label()
    val confirmRemoval: (String, () -> Unit) -> Unit = remember(selectedTabTitle) {
        { title, proceed ->
            pendingRemoval = PendingLibraryTvRemoval(
                title = title,
                listTitle = selectedTabTitle,
                proceed = proceed,
            )
        }
    }
    val tabCounts = remember(
        state.continueWatching,
        state.items,
    ) {
        LibraryTab.entries.associateWith { tab -> state.tvTabItemCount(tab) }
    }
    val hasFocusableGridContent = when (selectedTab) {
        LibraryTab.CONTINUE_WATCHING -> state.continueWatching.isNotEmpty()
        LibraryTab.HISTORY -> false
        else -> state.tabItems[selectedTab]?.isNotEmpty() == true
    }

    val preferredContentFocusRequester =
        if (hasFocusableGridContent) gridFocusRequester else selectedTabFocusRequester

    DisposableEffect(
        preferredContentFocusRequester,
        registerPreferredContentFocusRequester,
    ) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    LaunchedEffect(Unit) {
        effect.collect { event ->
            when (event) {
                LibraryState.Effect.ItemRemoved -> {
                    Toast.makeText(context, itemRemovedText, Toast.LENGTH_SHORT).show()
                }

                is LibraryState.Effect.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, hasFocusableGridContent) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnEvent(LibraryState.Event.ScreenResumed)
                if (restoreGridFocusOnResume && hasFocusableGridContent) {
                    restoreGridFocusOnResume = false
                    scope.launch {
                        requestFocusUntilTimeout(gridFocusRequester)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LibraryTopTabs(
            tabs = tvTabs,
            selectedTab = selectedTab,
            tabCounts = tabCounts,
            contentCanFocus = hasFocusableGridContent,
            onTabSelected = { onEvent(LibraryState.Event.TabSelected(it)) },
            contentFocusRequester = gridFocusRequester,
            tabFocusRequesters = tabFocusRequesters,
            mainMenuFocusRequester = mainMenuFocusRequester,
        )

        state.remoteError?.let { error ->
            ErrorBanner(
                tv = true,
                message = error.asString(),
                enabled = !state.isRemoteLoading,
                onRetry = { onEvent(LibraryState.Event.RetrySelected) },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val isInitialLoading = state.isLocalLoading(selectedTab) ||
                (
                    state.isRemoteLoading &&
                        state.remoteError == null &&
                        state.items.isEmpty() &&
                        state.continueWatching.isEmpty()
                    )
            if (isInitialLoading) {
                TvLoadingScreen()
                return@Box
            }
            when (selectedTab) {
                LibraryTab.CONTINUE_WATCHING -> ContinueWatchingGrid(
                    entries = state.continueWatching,
                    launchingAnimeId = state.launchingContinueWatchingAnimeId,
                    cardSize = state.continueWatchingCardSize,
                    gridFocusRequester = gridFocusRequester,
                    selectedTabFocusRequester = selectedTabFocusRequester,
                    focusStateKey = LibraryTab.CONTINUE_WATCHING.focusStateKey(
                        LibraryFocusSourceLocal,
                    ),
                    onEntrySelected = {
                        restoreGridFocusOnResume = true
                        onEvent(LibraryState.Event.ContinueWatchingSelected(it))
                    },
                    onDetailsSelected = {
                        restoreGridFocusOnResume = true
                        onEvent(LibraryState.Event.ContinueWatchingDetailsSelected(it))
                    },
                    onRemoveWatchProgress = {
                        onEvent(LibraryState.Event.RemoveWatchProgress(it))
                    },
                    confirmRemoval = confirmRemoval,
                )

                // Не показывается на ТВ: выше вкладка подменяется на первую из tvTabs.
                LibraryTab.HISTORY -> Unit

                LibraryTab.FAVORITES -> {
                    val favoriteItems = state.tabItems[LibraryTab.FAVORITES].orEmpty()
                    LibraryGrid(
                        tab = LibraryTab.FAVORITES,
                        items = favoriteItems,
                        gridFocusRequester = gridFocusRequester,
                        selectedTabFocusRequester = selectedTabFocusRequester,
                        focusStateKey = LibraryTab.FAVORITES.focusStateKey(
                            LibraryFocusSourceLocal,
                        ),
                        showTitleYear = state.showTitleYear,
                        onAnimeSelected = {
                            restoreGridFocusOnResume = true
                            onEvent(LibraryState.Event.AnimeSelected(it))
                        },
                        onRemoveEntry = {
                            onEvent(
                                LibraryState.Event.RemoveEntry(
                                    it,
                                    LibraryRemoveTarget.FAVORITE,
                                ),
                            )
                        },
                        confirmRemoval = confirmRemoval,
                    )
                }

                LibraryTab.WATCHING,
                LibraryTab.PLANNED,
                LibraryTab.COMPLETED,
                LibraryTab.POSTPONED,
                LibraryTab.DROPPED,
                -> {
                    val localItems = state.tabItems[selectedTab].orEmpty()
                    LibraryGrid(
                        tab = selectedTab,
                        items = localItems,
                        gridFocusRequester = gridFocusRequester,
                        selectedTabFocusRequester = selectedTabFocusRequester,
                        focusStateKey = selectedTab.focusStateKey(LibraryFocusSourceLocal),
                        showTitleYear = state.showTitleYear,
                        onAnimeSelected = {
                            restoreGridFocusOnResume = true
                            onEvent(LibraryState.Event.AnimeSelected(it))
                        },
                        onRemoveEntry = {
                            onEvent(
                                LibraryState.Event.RemoveEntry(
                                    it,
                                    LibraryRemoveTarget.LIST,
                                ),
                            )
                        },
                        confirmRemoval = confirmRemoval,
                    )
                }
            }
        }
    }

    pendingRemoval?.let { removal ->
        LibraryTvRemoveConfirmDialog(
            title = removal.title,
            listTitle = removal.listTitle,
            onConfirm = {
                pendingRemoval = null
                // Перенос фокуса на соседнюю карточку — после закрытия окна диалога, иначе
                // запрос уходит в окно, которое вот-вот исчезнет.
                scope.launch {
                    withFrameNanos { }
                    removal.proceed()
                }
            },
            onDismiss = { pendingRemoval = null },
        )
    }
}

private const val LibraryFocusSourceLocal = "local"
