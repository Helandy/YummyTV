package su.afk.yummy.tv.feature.details.mobile.viewingorder.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage
import su.afk.yummy.tv.core.designsystem.components.TimelineNodeBadge
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterType
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileRatingBadge
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.model.anime.urlsByQuality
import su.afk.yummy.tv.feature.details.mobile.details.view.drawSegment
import su.afk.yummy.tv.feature.details.presentation.R

/**
 * Строка вертикального таймлайна франшизы: слева нода с номером и линия (сплошная до текущего тайтла, дальше
 * пунктир), справа горизонтальная карточка. Линия продолжается на половину [gap] вверх и вниз, чтобы соседние
 * строки сливались в одну линию.
 */
@Composable
internal fun ViewingOrderTimelineRow(
    index: Int,
    currentIndex: Int,
    count: Int,
    item: AnimeViewingOrderItem,
    gap: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrent = index == currentIndex
    val primary = MaterialTheme.colorScheme.primary
    val passed = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val upcoming = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val shape = MaterialTheme.shapes.medium

    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(GutterWidth)
                .fillMaxHeight()
                .drawBehind {
                    val half = gap.toPx() / 2f
                    val cx = NodeColumnWidth.toPx() / 2f
                    val cy = NodeTopPadding.toPx() + NodeSize.toPx() / 2f
                    if (index > 0) {
                        drawSegment(index - 1, currentIndex, Offset(cx, -half), Offset(cx, cy), primary, passed, upcoming)
                    }
                    if (index < count - 1) {
                        // Под нодой стоит год: линия продолжается ниже подписи.
                        val start = if (item.year != null) cy + NodeSize.toPx() / 2f + YearLabelHeight.toPx() else cy
                        drawSegment(index, currentIndex, Offset(cx, start), Offset(cx, size.height + half), primary, passed, upcoming)
                    }
                },
            contentAlignment = Alignment.TopStart,
        ) {
            // Нода и год имеют фиксированные размеры, поэтому масштаб шрифта в колонке ограничен.
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = density.fontScale.coerceAtMost(MaxGutterFontScale)),
            ) {
                Column(
                    modifier = Modifier.width(NodeColumnWidth).padding(top = NodeTopPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimelineNodeBadge(
                        number = index + 1,
                        typeKind = item.typeKind,
                        current = isCurrent,
                        size = NodeSize,
                        textStyle = MaterialTheme.typography.labelSmall,
                    )
                    item.year?.let {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .then(if (isCurrent) Modifier.border(2.dp, primary, shape) else Modifier)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onClick),
        ) {
            Box(modifier = Modifier.width(PosterWidth).aspectRatio(TITLE_POSTER_ASPECT_RATIO)) {
                FallbackAsyncImage(
                    models = item.poster.urlsByQuality(LocalPosterQuality.current),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentScale = ContentScale.Crop,
                )
                item.rating?.let { MobileRatingBadge(rating = it, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) }
                item.type?.takeIf { it.isNotBlank() }?.let { MobilePosterType(it) }
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (isCurrent) {
                    Text(
                        text = stringResource(R.string.details_universe_you_are_here),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.relation.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.relation.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val GutterWidth = 48.dp
private val NodeSize = 26.dp
private val NodeColumnWidth = 48.dp

/** Высота подписи года под нодой: 4dp отступ + строка labelSmall. */
private val YearLabelHeight = 20.dp
private val NodeTopPadding = 12.dp
private val PosterWidth = 92.dp
private const val MaxGutterFontScale = 1.3f
