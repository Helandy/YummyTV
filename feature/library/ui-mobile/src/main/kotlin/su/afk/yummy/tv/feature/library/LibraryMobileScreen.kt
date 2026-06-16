package su.afk.yummy.tv.feature.library

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.model.PendingLibraryMobileRemoval
import su.afk.yummy.tv.feature.library.utils.libraryMobileTabs
import su.afk.yummy.tv.feature.library.utils.mobileTabItemCount
import su.afk.yummy.tv.feature.library.utils.toLibraryMobilePage
import su.afk.yummy.tv.feature.library.utils.toLibraryMobileTab
import su.afk.yummy.tv.feature.library.view.LibraryMobilePage
import su.afk.yummy.tv.feature.library.view.LibraryMobileRemoveConfirmDialog
import su.afk.yummy.tv.feature.library.view.LibraryMobileTabs

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryMobileScreen(

    state: LibraryState.State,
    effect: Flow<LibraryState.Effect>,
    onEvent: (LibraryState.Event) -> Unit,

) {
    val context = LocalContext.current
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
            }
        }
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
        topBar = { Text(stringResource(R.string.library_mobile_title)) },
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
