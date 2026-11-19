package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.utils.balancerFallbackIcon
import su.afk.yummy.tv.core.designsystem.utils.balancerLogoRes
import su.afk.yummy.tv.core.designsystem.utils.dubbingDisplayName
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.details.model.MobilePickerItem
import su.afk.yummy.tv.feature.details.mobile.details.view.MobilePickerItems
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.blocksNewDownload
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EpisodeDownloadBalancerSheet(
    selection: EpisodesState.EpisodeDownloadBalancerSelection,
    /** Выбранный плеер и состояние галки «запомнить». */
    onSelected: (video: AnimeVideo, rememberAsPreset: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var rememberAsPreset by remember { mutableStateOf(false) }
    BaseBottomSheet(
        onDismissRequest = onDismiss,
        titleContent = {
            EpisodeDownloadSheetTitle(
                title = stringResource(
                    R.string.details_balancer_title,
                    selection.episode,
                ),
                subtitle = selection.dubbing.dubbingDisplayName(),
            )
        },
    ) {
        if (selection.options.isEmpty()) {
            MobileMessage(
                title = stringResource(R.string.details_mobile_download_balancer_empty),
                icon = Icons.Filled.Info,
                fillMaxSize = false,
            )
        } else {
            EpisodeDownloadPresetToggle(
                checked = rememberAsPreset,
                onCheckedChange = { rememberAsPreset = it },
            )
            val items = remember(selection.options, rememberAsPreset) {
                selection.options.mapIndexed { index, option ->
                    MobilePickerItem(
                        key = "${option.video.id}|${option.video.iframeUrl}|$index",
                        title = option.title,
                        logoName = option.title,
                        logoRes = balancerLogoRes(option.title),
                        logoFallbackIcon = balancerFallbackIcon(option.title),
                        subtitle = option.subtitle,
                        enabled = !option.resolving && !option.status.blocksNewDownload(),
                        accentTitle = true,
                        trailing = { EpisodeDownloadStatusIcon(option.status, option.resolving) },
                        onClick = { onSelected(option.video, rememberAsPreset) },
                    )
                }
            }
            MobilePickerItems(items)
        }
    }
}
