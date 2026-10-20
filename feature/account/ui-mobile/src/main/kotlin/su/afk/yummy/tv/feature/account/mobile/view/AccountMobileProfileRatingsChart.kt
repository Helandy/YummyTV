package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice
import su.afk.yummy.tv.feature.account.mobile.account.utils.PROFILE_STATS_BODY_HEIGHT_DP
import su.afk.yummy.tv.feature.account.common.utils.toCompactCountLabel

private val LabelHeight = 14.dp
private val MinBarHeight = 3.dp
private val BarAreaHeight = PROFILE_STATS_BODY_HEIGHT_DP.dp - LabelHeight * 2 - 4.dp

/** Гистограмма распределения оценок: столбцы 1..10 слева направо, цвет столбца — цвет оценки. */
@Composable
internal fun AccountMobileProfileRatingsChart(
    slices: PersistentList<ProfileStatSlice>,
    modifier: Modifier = Modifier,
) {
    val ascending = slices.asReversed()
    val maxValue = ascending.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PROFILE_STATS_BODY_HEIGHT_DP.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        ascending.forEach { slice ->
            RatingBar(
                slice = slice,
                fraction = slice.value.toFloat() / maxValue.toFloat(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RatingBar(
    slice: ProfileStatSlice,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val hasValue = slice.value > 0L
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarAreaHeight + LabelHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (hasValue) slice.value.toCompactCountLabel() else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasValue) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.height(LabelHeight),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.dp)
                        .height(
                            if (hasValue) {
                                (BarAreaHeight * fraction).coerceAtLeast(MinBarHeight)
                            } else {
                                MinBarHeight
                            }
                        )
                        .background(
                            color = if (hasValue) {
                                slice.color
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                            },
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        ),
                )
            }
        }
        Text(
            text = slice.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 4.dp)
                .height(LabelHeight),
        )
    }
}
