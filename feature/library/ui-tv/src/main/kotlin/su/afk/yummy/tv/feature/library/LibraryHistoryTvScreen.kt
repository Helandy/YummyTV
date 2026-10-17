package su.afk.yummy.tv.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.library.history.LibraryHistoryState
import su.afk.yummy.tv.feature.library.view.LibraryTvHistoryPage

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
@Composable
private fun LibraryHistoryTvScreenDefaultPreview() = ScreenPreviewTheme {
    LibraryHistoryTvScreen(LibraryHistoryState.State(isSignedIn = true), emptyFlow()) {}
}

@Composable
fun LibraryHistoryTvScreen(
    state: LibraryHistoryState.State,
    @Suppress("UNUSED_PARAMETER") effect: Flow<LibraryHistoryState.Effect>,
    onEvent: (LibraryHistoryState.Event) -> Unit,
) {
    val listFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    var hasFocusableItems by remember { mutableStateOf(false) }

    // Фокус контента регистрируем, только когда есть карточка: пока история грузится, пуста
    // или недоступна гостю, ставить его некуда — иначе скаффолд ждёт и глотает «Назад».
    DisposableEffect(hasFocusableItems, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(
            if (hasFocusableItems) listFocusRequester else null,
        )
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnEvent(LibraryHistoryState.Event.ScreenResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        LibraryTvHistoryPage(
            history = state.watchHistory,
            localProgress = state.localProgress,
            isSignedIn = state.isSignedIn,
            gridFocusRequester = listFocusRequester,
            focusStateKey = HistoryFocusStateKey,
            onEntrySelected = { onEvent(LibraryHistoryState.Event.EntrySelected(it)) },
            onDetailsSelected = { onEvent(LibraryHistoryState.Event.DetailsSelected(it.animeId)) },
            onHasFocusableItemsChanged = { hasFocusableItems = it },
        )
    }
}

private const val HistoryFocusStateKey = "HISTORY_root"
