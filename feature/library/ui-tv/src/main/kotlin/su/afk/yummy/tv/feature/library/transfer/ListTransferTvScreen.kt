package su.afk.yummy.tv.feature.library.transfer

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.feature.library.model.ListTransferExportSummaryUi
import su.afk.yummy.tv.feature.library.model.ListTransferPreviewUi
import su.afk.yummy.tv.feature.library.model.ListTransferStatusCountUi
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.transfer.view.ListTransferTvEffects
import su.afk.yummy.tv.feature.library.transfer.view.ListTransferTvErrorBanner
import su.afk.yummy.tv.feature.library.transfer.view.ListTransferTvExportSection
import su.afk.yummy.tv.feature.library.transfer.view.ListTransferTvImportSection

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
@Composable
private fun ListTransferTvScreenDefaultPreview() = ScreenPreviewTheme {
    ListTransferTvScreen(
        state = ListTransferState.State(isSignedIn = true),
        effect = emptyFlow(),
        onEvent = {},
    )
}

@Preview(
    name = "PreviewAndExported",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
@Composable
private fun ListTransferTvScreenLoadedPreview() = ScreenPreviewTheme {
    ListTransferTvScreen(
        state = ListTransferState.State(
            isSignedIn = true,
            importPreview = ListTransferPreviewUi(
                format = ExternalListFormat.SHIKIMORI_JSON,
                totalCount = 210,
                statusCounts = persistentListOf(
                    ListTransferStatusCountUi(UserAnimeList.WATCHING, 8),
                    ListTransferStatusCountUi(UserAnimeList.COMPLETED, 160),
                    ListTransferStatusCountUi(UserAnimeList.PLANNED, 42),
                ),
                ratedCount = 134,
                withWatchedEpisodesCount = 168,
            ),
            exportSummary = ListTransferExportSummaryUi(
                format = ExternalListFormat.MAL_XML,
                exportedCount = 204,
                skippedWithoutExternalIdCount = 2,
                skippedFavoriteOnlyCount = 5,
            ),
        ),
        effect = emptyFlow(),
        onEvent = {},
    )
}

@Composable
fun ListTransferTvScreen(
    state: ListTransferState.State,
    effect: Flow<ListTransferState.Effect>,
    onEvent: (ListTransferState.Event) -> Unit,
) {
    ListTransferTvEffects(effect = effect, onEvent = onEvent)
    BackHandler { onEvent(ListTransferState.Event.BackClicked) }

    val initialFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { initialFocusRequester.requestFocus() }
    }

    val colors = MaterialTheme.colorScheme
    val errorText = state.errorMessage?.asString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_transfer_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
                Text(
                    text = stringResource(R.string.library_transfer_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(
                    if (state.isSignedIn) {
                        R.string.library_transfer_account_signed_in
                    } else {
                        R.string.library_transfer_account_guest
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .background(colors.surfaceContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (errorText != null) {
            ListTransferTvErrorBanner(
                message = errorText,
                onDismiss = { onEvent(ListTransferState.Event.DismissError) },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ListTransferTvImportSection(
                state = state,
                initialFocusRequester = initialFocusRequester,
                onEvent = onEvent,
                modifier = Modifier.weight(1.15f),
            )
            ListTransferTvExportSection(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.weight(0.85f),
            )
        }
    }
}
