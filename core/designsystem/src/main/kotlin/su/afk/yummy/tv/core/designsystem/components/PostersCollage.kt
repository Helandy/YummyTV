package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private const val STRIP_COUNT = 3
private val StripSlant = 28.dp
private val StripGap = 4.dp

/** Три постера коллекции в одну строку, разрезанные параллельными скосами «/». */
@Composable
fun PostersCollage(
    posterUrls: List<String>,
    modifier: Modifier = Modifier,
    slant: Dp = StripSlant,
    gap: Dp = StripGap,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val stripWidth = (maxWidth + slant * 2 - gap * 2) / STRIP_COUNT
        val pitch = stripWidth - slant + gap

        repeat(STRIP_COUNT) { index ->
            val url = posterUrls.getOrNull(index) ?: return@repeat
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = pitch * index)
                    .width(stripWidth)
                    .fillMaxHeight()
                    .clip(
                        SlantedStripShape(
                            slant = slant,
                            flatLeft = index == 0,
                            flatRight = index == STRIP_COUNT - 1,
                        ),
                    ),
            )
        }
        overlay()
    }
}
