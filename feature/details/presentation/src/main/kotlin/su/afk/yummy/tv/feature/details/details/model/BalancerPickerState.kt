package su.afk.yummy.tv.feature.details.details.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class BalancerPickerState(
    val episodeNumber: String,
    val options: ImmutableList<BalancerOption>,
)
