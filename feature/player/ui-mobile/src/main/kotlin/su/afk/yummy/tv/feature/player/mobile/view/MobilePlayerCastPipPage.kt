package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.presentation.R

/** Страница «PiP и Cast»: «картинка в картинке» и трансляция на другое устройство. */
@Composable
internal fun MobilePlayerCastPipPage(
    showPictureInPicture: Boolean,
    showCast: Boolean,
    onPictureInPicture: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MobilePlayerMenuPageHeader(title = stringResource(R.string.player_menu_cast_pip), onBack = onBack)
        if (showPictureInPicture) {
            MobilePlayerMenuRow(
                icon = Icons.Filled.PictureInPictureAlt,
                label = stringResource(R.string.player_mobile_pip),
                trailing = MobilePlayerMenuRowTrailing.None,
                onClick = onPictureInPicture,
            )
        }
        if (showCast) MobilePlayerCastRow()
    }
}
