package su.afk.yummy.tv.feature.library.mobile

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.mobile.model.PendingLibraryMobileRemoval
import su.afk.yummy.tv.feature.library.mobile.utils.libraryMobileTabs
import su.afk.yummy.tv.feature.library.mobile.utils.mobileTabItemCount
import su.afk.yummy.tv.feature.library.mobile.utils.toLibraryMobilePage
import su.afk.yummy.tv.feature.library.mobile.utils.toLibraryMobileTab
import su.afk.yummy.tv.feature.library.mobile.view.LibraryMobilePage
import su.afk.yummy.tv.feature.library.mobile.view.LibraryMobileRemoveConfirmDialog
import su.afk.yummy.tv.feature.library.mobile.view.LibraryMobileTabs

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        LibraryMobileScreen(LibraryState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryMobileScreen(

    state: LibraryState.State,
    effect: Flow<LibraryState.Effect>,
    onEvent: (LibraryState.Event) -> Unit,

    ) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    val itemRemovedText = stringResource(R.string.library_mobile_remove_success)
    val pagerState = rememberPagerState(
        initialPage = state.selectedTab.toLibraryMobilePage(),
        pageCount = { libraryMobileTabs.size },
    )
    val coroutineScope = rememberCoroutineScope()
    var pendingRemoval by remember { mutableStateOf<PendingLibraryMobileRemoval?>(null) }
    val tabCounts = remember(
        state.continueWatching,
        state.items,
    ) {
        libraryMobileTabs.associateWith { tab -> state.mobileTabItemCount(tab) }
    }

    LaunchedEffect(effect, context, itemRemovedText) {
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnEvent(LibraryState.Event.ScreenResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    pendingRemoval?.let { removal ->
        LibraryMobileRemoveConfirmDialog(
            title = removal.title,
            listTitle = removal.listTitle,
            onConfirm = {
                onEvent(removal.event())
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    LaunchedEffect(state.selectedTab) {
        val targetPage = state.selectedTab.toLibraryMobilePage()
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val selectedTab = pagerState.currentPage.toLibraryMobileTab()
        if (selectedTab != state.selectedTab) {
            onEvent(LibraryState.Event.TabSelected(selectedTab))
        }
    }

    BaseScreen(
        isScroll = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            LibraryMobileTabs(
                selectedTab = pagerState.currentPage.toLibraryMobileTab(),
                tabCounts = tabCounts,
                onSelected = { tab ->
                    val targetPage = tab.toLibraryMobilePage()
                    if (pagerState.currentPage != targetPage) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )

            HorizontalPager(
                state = pagerState,
                key = { page -> page.toLibraryMobileTab() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) { page ->
                LibraryMobilePage(
                    tab = page.toLibraryMobileTab(),
                    state = state,
                    onEvent = onEvent,
                    onRemovalRequested = { pendingRemoval = it },
                )
            }
        }
    }
}
