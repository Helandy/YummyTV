package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun BalancerDialog(
    picker: BalancerPickerState,
    onConfirmed: (AnimeVideo) -> Unit,
    onDismiss: () -> Unit,
) {
    MobilePickerBottomSheet(
        title = stringResource(R.string.details_mobile_balancer_title, picker.episodeNumber),
        onDismiss = onDismiss,
    ) {
        MobilePickerItems(
            items = picker.options.map { option ->
                MobilePickerItem(
                    key = "${option.playerName}:${option.video.id}",
                    title = if (option.isSupported) {
                        option.playerName
                    } else {
                        stringResource(
                            R.string.details_mobile_unsupported_player,
                            option.playerName
                        )
                    },
                    enabled = option.isSupported,
                    onClick = { onConfirmed(option.video) },
                )
            },
        )
    }
}
