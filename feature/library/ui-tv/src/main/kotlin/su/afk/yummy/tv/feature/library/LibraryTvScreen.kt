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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.feature.library.utils.libraryTabsDisplayOrder
import su.afk.yummy.tv.feature.library.utils.tvTabItemCount
import su.afk.yummy.tv.feature.library.utils.userAnimeListId
import su.afk.yummy.tv.feature.library.view.ContinueWatchingGrid
import su.afk.yummy.tv.feature.library.view.LibraryGrid
import su.afk.yummy.tv.feature.library.view.LibraryRemoteErrorBanner
import su.afk.yummy.tv.feature.library.view.LibraryTopTabs

@Composable
fun LibraryTvScreen(
    state: LibraryState.State,
    effect: Flow<LibraryState.Effect>,
    onEvent: (LibraryState.Event) -> Unit,
) {
    val gridFocusRequester = remember { FocusRequester() }
    val tabFocusRequesters = remember {
        libraryTabsDisplayOrder().associateWith { FocusRequester() }
    }
    val selectedTabFocusRequester = tabFocusRequesters.getValue(state.selectedTab)
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val itemRemovedText = stringResource(R.string.library_remove_success)
    val scope = rememberCoroutineScope()
    val currentOnEvent by rememberUpdatedState(onEvent)
    var restoreGridFocusOnResume by rememberSaveable { mutableStateOf(false) }
    val tabCounts = remember(
        state.continueWatching,
        state.items,
    ) {
        libraryTabsDisplayOrder().associateWith { tab -> state.tvTabItemCount(tab) }
    }
    val hasFocusableGridContent = when (state.selectedTab) {
        LibraryTab.CONTINUE_WATCHING -> state.continueWatching.isNotEmpty()
        LibraryTab.FAVORITES -> state.items.any { it.isFavorite }

        LibraryTab.PLANNED,
        LibraryTab.COMPLETED,
        LibraryTab.DROPPED,
        LibraryTab.POSTPONED,
        LibraryTab.WATCHING -> {
            val localListId = state.selectedTab.userAnimeListId()
            localListId != null && state.items.any { it.listId == localListId }
        }
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

    LaunchedEffect(effect, context, itemRemovedText) {
        effect.collect { event ->
            when (event) {
                LibraryState.Effect.ItemRemoved -> {
                    Toast.makeText(context, itemRemovedText, Toast.LENGTH_SHORT).show()
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
                        withFrameNanos { }
                        runCatching { gridFocusRequester.requestFocus() }
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
            selectedTab = state.selectedTab,
            tabCounts = tabCounts,
            contentCanFocus = hasFocusableGridContent,
            onTabSelected = { onEvent(LibraryState.Event.TabSelected(it)) },
            contentFocusRequester = gridFocusRequester,
            tabFocusRequesters = tabFocusRequesters,
            mainMenuFocusRequester = mainMenuFocusRequester,
        )

        state.remoteError?.let { error ->
            LibraryRemoteErrorBanner(
                message = error,
                isLoading = state.isRemoteLoading,
                onRetry = { onEvent(LibraryState.Event.RetrySelected) },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (state.selectedTab) {
                LibraryTab.CONTINUE_WATCHING -> ContinueWatchingGrid(
                    entries = state.continueWatching,
                    cardSize = state.continueWatchingCardSize,
                    gridFocusRequester = gridFocusRequester,
                    selectedTabFocusRequester = selectedTabFocusRequester,
                    focusStateKey = LibraryTab.CONTINUE_WATCHING.focusStateKey(
                        LibraryFocusSourceLocal
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
                )

                LibraryTab.FAVORITES -> {
                    val favoriteItems =
                        remember(state.items) { state.items.filter { it.isFavorite } }
                    LibraryGrid(
                        tab = LibraryTab.FAVORITES,
                        items = favoriteItems,
                        gridFocusRequester = gridFocusRequester,
                        selectedTabFocusRequester = selectedTabFocusRequester,
                        focusStateKey = LibraryTab.FAVORITES.focusStateKey(
                            LibraryFocusSourceLocal
                        ),
                        onAnimeSelected = {
                            restoreGridFocusOnResume = true
                            onEvent(LibraryState.Event.AnimeSelected(it))
                        },
                        onRemoveEntry = {
                            onEvent(
                                LibraryState.Event.RemoveEntry(
                                    it,
                                    LibraryRemoveTarget.FAVORITE,
                                )
                            )
                        },
                    )
                }

                LibraryTab.WATCHING,
                LibraryTab.PLANNED,
                LibraryTab.COMPLETED,
                LibraryTab.POSTPONED,
                LibraryTab.DROPPED -> {
                    val localListId = state.selectedTab.userAnimeListId()
                    val localItems = remember(state.items, localListId) {
                        state.items.filter { it.listId == localListId }
                    }
                    LibraryGrid(
                        tab = state.selectedTab,
                        items = localItems,
                        gridFocusRequester = gridFocusRequester,
                        selectedTabFocusRequester = selectedTabFocusRequester,
                        focusStateKey = state.selectedTab.focusStateKey(LibraryFocusSourceLocal),
                        onAnimeSelected = {
                            restoreGridFocusOnResume = true
                            onEvent(LibraryState.Event.AnimeSelected(it))
                        },
                        onRemoveEntry = {
                            onEvent(
                                LibraryState.Event.RemoveEntry(
                                    it,
                                    LibraryRemoveTarget.LIST,
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

private const val LibraryFocusSourceLocal = "local"

private fun LibraryTab.focusStateKey(source: String): String = "${name}_$source"
