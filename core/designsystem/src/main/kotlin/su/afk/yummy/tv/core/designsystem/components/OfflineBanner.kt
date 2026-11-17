package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.locals.LocalYaniMirrorActive

/**
 * Плашка «нет сети»; когда сеть есть, но yani недоступен и данные идут из копии YummyTV ([LocalYaniMirrorActive]) — информационная
 * плашка о резервной копии. Встраивается в вёрстку главной (над поиском на мобилке, под
 * «Продолжить просмотр» на ТВ), а не рисуется оверлеем: глобальная плашка перекрывала
 * плеер и другие экраны. Появляясь, раздвигает контент, а не наезжает на него.
 */
@Composable
fun OfflineBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    val mirrorActive = LocalYaniMirrorActive.current
    AnimatedVisibility(
        visible = isOffline || mirrorActive,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(260, delayMillis = 120)),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
            shape = RoundedCornerShape(percent = 50),
            color = if (isOffline) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isOffline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(if (isOffline) R.string.offline_banner_message else R.string.yani_mirror_banner_message),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
