package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileSectionLoading

@Composable
internal fun LibraryMobileLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    MobileSectionLoading(modifier = modifier)
}
