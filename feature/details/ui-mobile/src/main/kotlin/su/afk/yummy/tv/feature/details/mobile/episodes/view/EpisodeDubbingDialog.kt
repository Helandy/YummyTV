package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.utils.dubbingLogoRes
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.details.model.MobilePickerItem
import su.afk.yummy.tv.feature.details.mobile.details.view.MobilePickerBottomSheet
import su.afk.yummy.tv.feature.details.mobile.details.view.MobilePickerItems
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun EpisodeDubbingDialog(
    selection: EpisodesState.EpisodeDubbingSelection,
    onSelected: (AnimeVideo) -> Unit,
    onDismiss: () -> Unit,
) {
    MobilePickerBottomSheet(
        title = stringResource(R.string.details_episode_number, selection.episode),
        onDismiss = onDismiss,
    ) {
        MobilePickerItems(
            items = selection.options.map { option ->
                MobilePickerItem(
                    key = option.item.name,
                    title = option.item.name,
                    logoName = option.item.name,
                    logoRes = dubbingLogoRes(option.item.name),
                    logoFallbackIcon = Icons.Filled.Mic,
                    subtitle = option.item.supportedBalancers,
                    views = option.item.views,
                    episodeCount = option.item.episodeCount,
                    accentTitle = true,
                    onClick = { onSelected(option.video) },
                )
            },
        )
    }
}
