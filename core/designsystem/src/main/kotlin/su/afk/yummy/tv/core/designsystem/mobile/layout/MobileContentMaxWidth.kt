package su.afk.yummy.tv.core.designsystem.mobile.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Предел ширины одноколоночного контента: дальше строки текста и карточки читаются хуже. */
val MobileContentMaxWidth: Dp = 840.dp

/**
 * Центрирует одноколоночный контент и не даёт ему растянуться шире [MobileContentMaxWidth]
 * на планшете, развёрнутом складном и в широком окне. На телефоне ничего не меняет.
 * Ставить первым в цепочке, чтобы следующие `fillMaxSize`/`padding` считались от уже
 * ограниченной ширины.
 */
fun Modifier.mobileContentMaxWidth(): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = MobileContentMaxWidth)
    .fillMaxWidth()
