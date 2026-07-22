package su.afk.yummy.tv.feature.details.similar.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvRetryButton
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun TvSimilarRecommendationVisibilityButton(
    ignored: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvRetryButton(
        text = stringResource(
            if (ignored) R.string.details_restore_recommendation
            else R.string.details_ignore_recommendation
        ),
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}
