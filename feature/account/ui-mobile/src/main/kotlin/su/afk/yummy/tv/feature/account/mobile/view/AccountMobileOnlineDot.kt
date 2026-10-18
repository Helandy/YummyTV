package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors

/** Точка статуса у аватара: зелёная — в сети, красная — нет; кольцо цвета фона отделяет её от фото. */
@Composable
internal fun AccountMobileOnlineDot(
    isOnline: Boolean,
    size: Dp,
    ring: Dp,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ringColor)
            .padding(ring)
            .clip(CircleShape)
            .background(if (isOnline) YummySemanticColors.UserOnline else YummySemanticColors.UserOffline),
    )
}
