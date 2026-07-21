package su.afk.yummy.tv.feature.details.mobile.similar.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun MobileSimilarRecommendationVisibilityButton(
    ignored: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(
                if (ignored) R.string.details_mobile_restore_recommendation
                else R.string.details_mobile_ignore_recommendation
            )
        )
    }
}
