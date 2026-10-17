package su.afk.yummy.tv.core.designsystem.mobile.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.preview.MobileScreenPreview
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme

/** Центрированный лоадер для секции/вкладки на мобилке. */
@Composable
fun MobileSectionLoading(
    modifier: Modifier = Modifier,
    minHeight: Dp = 180.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        contentAlignment = Alignment.Center,
    ) {
        YummyLoadingIndicator()
    }
}

@MobileScreenPreview
@Composable
private fun MobileSectionLoadingPreview() {
    ScreenPreviewTheme {
        MobileSectionLoading()
    }
}
