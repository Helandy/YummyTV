package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.feature.player.mobile.utils.mobileResizeIcon
import su.afk.yummy.tv.feature.player.mobile.utils.mobileResizeLabelRes
import su.afk.yummy.tv.feature.player.mobile.utils.mobileResizeModeOrder

/** Режим масштаба видео: иконки в один ряд, под ними — название выбранного режима. */
@Composable
internal fun MobilePlayerScaleSelector(
    selectedMode: PlayerResizeMode,
    onModeSelected: (PlayerResizeMode) -> Unit,
) {
    MobilePlayerIconChoiceRow(
        options = mobileResizeModeOrder,
        selected = selectedMode,
        icon = { it.mobileResizeIcon() },
        label = { stringResource(it.mobileResizeLabelRes()) },
        onSelected = onModeSelected,
    )
}
