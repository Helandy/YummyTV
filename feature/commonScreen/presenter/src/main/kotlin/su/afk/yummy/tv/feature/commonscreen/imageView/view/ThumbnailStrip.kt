package su.afk.yummy.tv.feature.commonscreen.imageView.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.commonscreen.R
import su.afk.yummy.tv.feature.commonscreen.imageView.*

@Composable
internal fun ThumbnailStrip(
    images: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }
    val thumbShape = RoundedCornerShape(6.dp)

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(images) { index, url ->
            val isSelected = index == selectedIndex
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 90.dp, height = 60.dp)
                    .clip(thumbShape)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = thumbShape,
                    )
                    .tvFocusableClick(onClick = { onSelect(index) }, shape = thumbShape),
            )
        }
    }
}
