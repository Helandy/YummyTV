package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.details.details.model.DubbingOption
import su.afk.yummy.tv.feature.details.details.model.DubbingPickerState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.details.model.MobilePickerItem

@Composable
internal fun DubbingDialog(
    picker: DubbingPickerState,
    onSelected: (DubbingOption) -> Unit,
    onDismiss: () -> Unit,
) {
    MobilePickerBottomSheet(
        title = stringResource(R.string.details_mobile_episode_dubbings_title, picker.episode),
        onDismiss = onDismiss,
    ) {
        MobilePickerItems(
            items = picker.options.map { option ->
                MobilePickerItem(
                    key = option.item.name,
                    title = option.item.name,
                    subtitle = option.item.supportedBalancers,
                    views = option.item.views,
                    episodeCount = option.item.episodeCount,
                    onClick = { onSelected(option) },
                )
            },
        )
    }
}
