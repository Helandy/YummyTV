package su.afk.yummy.tv.feature.details.full

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMetaRow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.feature.details.view.DetailsMobileScaffold

@Composable
fun FullDetailsMobileScreen(
    state: FullDetailsState.State,
    effect: Flow<FullDetailsState.Effect>,
    onEvent: (FullDetailsState.Event) -> Unit,
) {
    DetailsMobileScaffold(
        title = state.details?.title ?: "Описание",
        onBack = { onEvent(FullDetailsState.Event.BackSelected) },
    ) { padding ->
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { onEvent(FullDetailsState.Event.RetrySelected) },
            empty = state.details == null,
        ) {
            val details = state.details
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Text(details?.description.orEmpty()) }
                item { MobileMetaRow("Другие названия", details?.otherTitles.orEmpty().joinToString()) }
                item { MobileMetaRow("Студии", details?.studios.orEmpty().joinToString { it.title }) }
                item { MobileMetaRow("Создатели", details?.creators.orEmpty().joinToString { it.title }) }
            }
        }
    }
}
