package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme

/**
 * «Моя оценка»: звезда и цифра цветом «светофора» на тёмной полупрозрачной капсуле,
 * чтобы читаться поверх постера и размытого фона.
 */
@Composable
fun UserRatingChip(
    rating: Int,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    val contentColor = rating.toDouble().toRatingTextColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(color = CAPSULE_COLOR, shape = CircleShape)
            .padding(start = 10.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
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

private val CAPSULE_COLOR = Color.Black.copy(alpha = 0.45f)

@Preview(showBackground = true, backgroundColor = 0xFF8A3B4A)
@Composable
private fun UserRatingChipPreview() {
    ScreenPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserRatingChip(rating = 3, textStyle = MaterialTheme.typography.titleMedium)
            UserRatingChip(rating = 7, textStyle = MaterialTheme.typography.titleMedium)
            UserRatingChip(rating = 10, textStyle = MaterialTheme.typography.titleMedium)
        }
    }
}
