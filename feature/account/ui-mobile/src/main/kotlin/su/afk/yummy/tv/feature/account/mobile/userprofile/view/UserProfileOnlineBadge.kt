package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.time.rememberNowEpochSeconds
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.feature.account.common.utils.isOnlineAt
import su.afk.yummy.tv.feature.account.common.utils.lastOnlineLabel
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileOnlineDot

/** Точка статуса у аватара чужого профиля; по тапу — подсказка «В сети» / «Был(а) в сети …». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserProfileOnlineBadge(
    lastOnlineSeconds: Long,
    sex: UserProfileSex,
    modifier: Modifier = Modifier,
) {
    val nowEpochSeconds = rememberNowEpochSeconds()
    val label = lastOnlineSeconds.lastOnlineLabel(sex, nowEpochSeconds) ?: return
    val tooltipState = rememberTooltipState(isPersistent = false)
    val scope = rememberCoroutineScope()
    // TooltipBox не пробрасывает align родительского Box, поэтому позиционируем обёртку.
    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(label) } },
            state = tooltipState,
        ) {
            AccountMobileOnlineDot(
                isOnline = lastOnlineSeconds.isOnlineAt(nowEpochSeconds),
                size = 24.dp,
                ring = 4.dp,
                ringColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .clickable { scope.launch { tooltipState.show() } },
            )
        }
    }
}
