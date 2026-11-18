package su.afk.yummy.tv.feature.library.mobile.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.UnmatchedExternalAnime
import su.afk.yummy.tv.feature.library.mobile.transfer.view.ListTransferMobileEffects
import su.afk.yummy.tv.feature.library.mobile.transfer.view.ListTransferMobileErrorBanner
import su.afk.yummy.tv.feature.library.mobile.transfer.view.ListTransferMobileExportCard
import su.afk.yummy.tv.feature.library.mobile.transfer.view.ListTransferMobileImportCard
import su.afk.yummy.tv.feature.library.model.ListTransferImportSummaryUi
import su.afk.yummy.tv.feature.library.model.ListTransferPreviewUi
import su.afk.yummy.tv.feature.library.model.ListTransferStatusCountUi
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.transfer.ListTransferState

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun ListTransferMobileScreenDefaultPreview() = ScreenPreviewTheme {
    ListTransferMobileScreen(
        state = ListTransferState.State(isSignedIn = true),
        effect = emptyFlow(),
        onEvent = {},
    )
}

@Preview(name = "PreviewAndSummary", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun ListTransferMobileScreenLoadedPreview() = ScreenPreviewTheme {
    ListTransferMobileScreen(
        state = ListTransferState.State(
            isSignedIn = true,
            importPreview = ListTransferPreviewUi(
                format = ExternalListFormat.MAL_XML,
                totalCount = 148,
                statusCounts = persistentListOf(
                    ListTransferStatusCountUi(UserAnimeList.WATCHING, 12),
                    ListTransferStatusCountUi(UserAnimeList.COMPLETED, 96),
                    ListTransferStatusCountUi(UserAnimeList.PLANNED, 40),
                ),
                ratedCount = 87,
                withWatchedEpisodesCount = 108,
            ),
            importSummary = ListTransferImportSummaryUi(
                addedCount = 42,
                updatedCount = 5,
                skippedExistingCount = 99,
                failedCount = 0,
                unmatched = persistentListOf(
                    UnmatchedExternalAnime(
                        externalId = 59999,
                        title = "Unknown Special OVA",
                        list = UserAnimeList.PLANNED,
                    ),
                ),
            ),
        ),
        effect = emptyFlow(),
        onEvent = {},
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ListTransferMobileScreen(
    state: ListTransferState.State,
    effect: Flow<ListTransferState.Effect>,
    onEvent: (ListTransferState.Event) -> Unit,
) {
    ListTransferMobileEffects(effect = effect, onEvent = onEvent)

    val title = stringResource(R.string.library_transfer_title)
    val errorText = state.errorMessage?.asString()

    BaseScreen(
        isScroll = false,
        contentModifier = Modifier.navigationBarsPadding(),
        customTopBar = {
            MobileTopBar(
                title = title,
                onBack = { onEvent(ListTransferState.Event.BackClicked) },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.mobileContentMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.library_transfer_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.isSignedIn) {
                                    R.string.library_transfer_account_signed_in
                                } else {
                                    R.string.library_transfer_account_guest
                                },
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (errorText != null) {
                item(key = "error") {
                    ListTransferMobileErrorBanner(
                        message = errorText,
                        onDismiss = { onEvent(ListTransferState.Event.DismissError) },
                    )
                }
            }

            item(key = "import") {
                ListTransferMobileImportCard(state = state, onEvent = onEvent)
            }

            item(key = "export") {
                ListTransferMobileExportCard(state = state, onEvent = onEvent)
            }
        }
    }
}
