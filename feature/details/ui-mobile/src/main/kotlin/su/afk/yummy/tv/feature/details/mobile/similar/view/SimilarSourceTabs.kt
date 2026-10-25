package su.afk.yummy.tv.feature.details.mobile.similar.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTextTabs
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun SimilarSourceTabs(
    fromAi: Boolean,
    onSourceSelected: (fromAi: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    MobileTextTabs(
        titles = listOf(
            stringResource(R.string.details_mobile_similar_source_site),
            stringResource(R.string.details_mobile_similar_source_ai),
        ),
        selectedIndex = if (fromAi) 1 else 0,
        onSelected = { onSourceSelected(it == 1) },
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    )
}
