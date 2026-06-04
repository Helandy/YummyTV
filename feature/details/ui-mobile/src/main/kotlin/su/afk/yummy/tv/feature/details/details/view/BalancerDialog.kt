package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun BalancerDialog(
    picker: BalancerPickerState,
    onConfirmed: (su.afk.yummy.tv.domain.anime.model.AnimeVideo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.details_mobile_balancer_title, picker.episodeNumber))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                picker.options.forEach { option ->
                    FilledTonalButton(
                        enabled = option.isSupported,
                        onClick = { onConfirmed(option.video) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (option.isSupported) {
                                option.playerName
                            } else {
                                stringResource(R.string.details_mobile_unsupported_player, option.playerName)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.details_mobile_cancel))
            }
        },
    )
}
