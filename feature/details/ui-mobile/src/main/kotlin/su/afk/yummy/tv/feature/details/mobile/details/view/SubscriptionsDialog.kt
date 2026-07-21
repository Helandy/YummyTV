package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun SubscriptionsDialog(
    state: DetailsState.State,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_mobile_subscriptions)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.isSubscriptionsLoading && state.subscriptions.isEmpty() -> {
                        Text(stringResource(R.string.details_mobile_subscriptions_loading))
                    }

                    state.subscriptions.isEmpty() -> {
                        Text(stringResource(R.string.details_mobile_subscriptions_empty))
                    }

                    else -> state.subscriptions.forEach { option ->
                        FilledTonalButton(
                            onClick = { onToggle(option.key) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = option.dubbing,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.details_mobile_subscription_meta,
                                        option.player,
                                        option.episodesCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = stringResource(
                                    if (option.isSubscribed) {
                                        R.string.details_mobile_unsubscribe
                                    } else {
                                        R.string.details_mobile_subscribe
                                    },
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.details_mobile_close))
            }
        },
    )
}
