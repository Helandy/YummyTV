package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.details.model.MobilePickerItem
import su.afk.yummy.tv.feature.details.mobile.details.view.MobilePickerItems
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EpisodeDownloadQualitySheet(
    selection: EpisodesState.EpisodeDownloadQualitySelection,
    onSelected: (EpisodesState.EpisodeDownloadQualityOption) -> Unit,
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(
        onDismissRequest = onDismiss,
        titleContent = {
            EpisodeDownloadSheetTitle(
                title = stringResource(R.string.details_mobile_download_quality_title),
                subtitle = stringResource(R.string.details_mobile_download_quality_prompt),
            )
        },
    ) {
        if (selection.options.isEmpty()) {
            MobileMessage(
                title = stringResource(R.string.details_mobile_download_quality_empty),
                icon = Icons.Filled.Info,
                fillMaxSize = false,
            )
        } else {
            val items = remember(selection.options) {
                selection.options.map { option ->
                    MobilePickerItem(
                        key = "${option.label}|${option.url}",
                        title = option.label,
                        onClick = { onSelected(option) },
                    )
                }
            }
            MobilePickerItems(items)
        }
    }
}
