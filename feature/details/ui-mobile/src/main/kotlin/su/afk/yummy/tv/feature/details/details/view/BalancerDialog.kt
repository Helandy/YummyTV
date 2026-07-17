package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.MobilePickerItem
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
        if (picker.preferredPlayerUnavailable) {
            Text(
                text = stringResource(R.string.details_mobile_balancer_preferred_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
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
                    subtitle = option.video.dubbing,
                    views = option.video.views,
                    enabled = option.isSupported,
                    onClick = { onConfirmed(option.video) },
                )
            },
        )
    }
}
