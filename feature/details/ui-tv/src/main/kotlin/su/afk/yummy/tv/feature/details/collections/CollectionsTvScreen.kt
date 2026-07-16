package su.afk.yummy.tv.feature.details.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.collections.view.CollectionsGrid
import su.afk.yummy.tv.feature.details.collections.view.CollectionsMessage

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun CollectionsTvScreenDefaultPreview() = ScreenPreviewTheme {
    CollectionsTvScreen(CollectionsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(
    name = "Loading",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
private fun CollectionsTvScreenLoadingPreview() = ScreenPreviewTheme {
    CollectionsTvScreen(CollectionsState.State(isLoading = true), emptyFlow()) {}
}

@Preview(
    name = "Error",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun CollectionsTvScreenErrorPreview() = ScreenPreviewTheme {
    CollectionsTvScreen(
        CollectionsState.State(
            isLoading = false,
            error = "Не удалось загрузить коллекции"
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollectionsTvScreen(

    state: CollectionsState.State,
    effect: Flow<CollectionsState.Effect>,
    onEvent: (CollectionsState.Event) -> Unit,

    ) {
    fun handleBack() {
        onEvent(CollectionsState.Event.BackSelected)
    }

    BackHandler { handleBack() }
    val error = state.error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Back, Key.Escape -> {
                        handleBack()
                        true
                    }

                    else -> false
                }
            }
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading -> TvLoadingScreen()
            error != null -> CollectionsMessage(
                text = error,
                onRetry = { onEvent(CollectionsState.Event.RetrySelected) },
                modifier = Modifier.align(Alignment.Center),
            )

            state.collections.isEmpty() -> TvStateMessage(
                title = stringResource(R.string.details_collections_empty),
                icon = Icons.Outlined.Collections,
            )

            else -> CollectionsGrid(
                collections = state.collections,
                onCollectionSelected = { onEvent(CollectionsState.Event.CollectionSelected(it)) },
            )
        }
    }
}
