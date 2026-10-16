package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** «Моя оценка» без подписей и подложки: звезда и цифра цветом «светофора» как у остальных оценок. */
@Composable
fun UserRatingChip(
    rating: Int,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    fontWeight: FontWeight = FontWeight.ExtraBold,
) {
    val contentColor = rating.toDouble().toRatingTextColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = rating.toString(),
            style = textStyle,
            fontWeight = fontWeight,
            color = contentColor,
            maxLines = 1,
        )
    }
}
