package su.afk.yummy.tv.feature.details.mobile.rating.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun MobileRatingPicker(
    selectedRating: Int?,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratings = (10 downTo 1).toList()
    val listState = rememberLazyListState()

    LaunchedEffect(selectedRating) {
        val index = ratings.indexOf(selectedRating)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.details_mobile_rating_pick),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(ratings, key = { it }) { rating ->
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        MobileRatingButton(
                            rating = rating,
                            selected = selectedRating == rating,
                            onClick = { onRatingSelected(rating) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileRatingButton(
    rating: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val foreground =
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.size(width = 72.dp, height = 52.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = rating.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = foreground,
            )
        }
    }
}
