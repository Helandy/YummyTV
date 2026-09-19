package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.details.model.MobilePickerItem
import su.afk.yummy.tv.feature.details.mobile.details.view.MobilePickerBottomSheet
import su.afk.yummy.tv.feature.details.mobile.details.view.MobilePickerItems

@Composable
internal fun EpisodeDubbingDialog(
    selection: EpisodesState.EpisodeDubbingSelection,
    onSelected: (AnimeVideo) -> Unit,
    onDismiss: () -> Unit,
) {
    MobilePickerBottomSheet(
        title = stringResource(R.string.details_mobile_episode_dubbings_title, selection.episode),
        onDismiss = onDismiss,
    ) {
        MobilePickerItems(
            items = selection.options.map { option ->
                MobilePickerItem(
                    key = option.item.name,
                    title = option.item.name,
                    subtitle = option.item.supportedBalancers,
                    views = option.item.views,
                    accentTitle = true,
                    onClick = { onSelected(option.video) },
                )
            },
        )
    }
}
