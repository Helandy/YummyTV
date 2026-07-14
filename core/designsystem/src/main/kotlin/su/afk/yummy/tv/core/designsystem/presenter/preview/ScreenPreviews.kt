package su.afk.yummy.tv.core.designsystem.presenter.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummyTvTheme

@Preview(
    name = "Mobile",
    device = "spec:width=412dp,height=915dp,dpi=420",
    showBackground = true,
)
annotation class MobileScreenPreview

@Preview(
    name = "TV",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
annotation class TvScreenPreview

@Composable
fun ScreenPreviewTheme(content: @Composable () -> Unit) {
    YummyTvTheme(content = content)
}
