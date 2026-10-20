package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import su.afk.yummy.tv.core.designsystem.components.QrCode
import su.afk.yummy.tv.domain.account.model.LocalAuthCode
import su.afk.yummy.tv.domain.account.utils.encodeLocalAuthPairingPayload

/**
 * QR и код сопряжения рядом: каждая группа кода на своей строке, три строки по высоте равны QR.
 *
 * Блок подстраивается под доступную высоту, но не больше [MaxQrSize]: на русском подзаголовок
 * переносится в три строки, а над кнопкой может появиться ошибка — фиксированный размер тогда
 * сплющивал кнопку «Назад» внизу панели.
 */
@Composable
internal fun LocalAuthPairingCode(
    serviceName: String,
    pin: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val qrSize = min(maxHeight, MaxQrSize)
        val lineHeight = with(LocalDensity.current) { (qrSize / CodeLines).toSp() }
        val cellWidth = qrSize / CodeLines * CellWidthRatio
        Row(
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QrCode(
                content = encodeLocalAuthPairingPayload(serviceName, pin),
                size = qrSize,
            )
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                pin.chunked(LocalAuthCode.GROUP_SIZE).forEach { group ->
                    Row {
                        // Шрифт пропорциональный: «I» и «M» разной ширины, строки кода выходили разной длины.
                        // Каждому символу — ячейка одной ширины.
                        group.forEach { char ->
                            Text(
                                text = char.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = lineHeight * FontToLineRatio,
                                    lineHeight = lineHeight,
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.width(cellWidth),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val MaxQrSize: Dp = 220.dp
private const val CodeLines = LocalAuthCode.LENGTH / LocalAuthCode.GROUP_SIZE

/** 56sp шрифта на 72sp строки — пропорция, при которой строки кода не слипаются. */
private const val FontToLineRatio = 56f / 72f

/** Ширина ячейки символа к высоте строки: с запасом под самую широкую букву («M», «W»). */
private const val CellWidthRatio = 0.78f
