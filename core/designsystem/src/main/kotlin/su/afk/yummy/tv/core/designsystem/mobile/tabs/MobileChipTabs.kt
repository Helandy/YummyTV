package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * Компактные вкладки ровной сеткой: все видны сразу, ячейки одинаковой ширины
 * (8 вкладок → 4×2, 6 → 3×2). В ячейке иконка со счётчиком и подпись под ними.
 * Одни и те же в библиотеке и во вкладке «Списки» профиля.
 *
 * Если две вкладки соседствуют по горизонтали или вертикали, подсветка плавно перетекает
 * из одной плитки в другую (вместе с оттенком акцента); для несоседних плиток и переноса
 * строки используется мягкое перекрёстное затухание без диагонального пролёта.
 */
@Composable
fun <K> MobileChipTabs(
    tabs: List<MobileChipTab<K>>,
    selectedKey: K,
    onSelected: (K) -> Unit,
    modifier: Modifier = Modifier,
    transition: MobileTabTransition? = null,
) {
    val columns = chipTabColumns(tabs.size)
    val rows = tabs.chunked(columns)
    val selectedIndex = tabs.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val resolvedTransition = transition ?: rememberAnimatedTabTransition(selectedIndex, tabs.size)
    val rowOffsets = remember(rows.size) {
        mutableStateListOf<Offset>().apply { repeat(rows.size) { add(Offset.Zero) } }
    }
    val itemBoundsInRow = remember(tabs.size, columns) {
        mutableStateListOf<Rect?>().apply { repeat(tabs.size) { add(null) } }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = CONTAINER_ALPHA),
    ) {
        Column(
            modifier = Modifier
                .padding(4.dp)
                .drawBehind {
                    if (tabs.isEmpty()) return@drawBehind
                    val resolvedBounds = List(tabs.size) { index ->
                        val local = itemBoundsInRow.getOrNull(index) ?: return@List null
                        val rowOffset = rowOffsets.getOrNull(index / columns) ?: Offset.Zero
                        local.translate(rowOffset)
                    }
                    drawFluidPillIndicator(
                        bounds = resolvedBounds,
                        transition = resolvedTransition,
                        cornerRadius = CornerRadius(CHIP_CORNER_RADIUS.toPx()),
                        colorForIndex = { index ->
                            tabs.getOrNull(index)?.color?.copy(alpha = SELECTED_CHIP_ALPHA) ?: Color.Transparent
                        },
                        areAdjacent = { from, to -> areAdjacentGridTabs(from, to, columns) },
                    )
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .onPlaced { coordinates ->
                            val offset = coordinates.positionInParent()
                            if (rowOffsets[rowIndex] != offset) {
                                rowOffsets[rowIndex] = offset
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    row.forEachIndexed { columnIndex, tab ->
                        val tabIndex = rowIndex * columns + columnIndex
                        MobileChipTabItem(
                            tab = tab,
                            selected = tabIndex == resolvedTransition.activeIndex,
                            selectionFraction = resolvedTransition.selectionFraction(tabIndex),
                            onClick = { onSelected(tab.key) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onPlaced { coordinates ->
                                    val rect = coordinates.boundsInParent()
                                    if (itemBoundsInRow[tabIndex] != rect) {
                                        itemBoundsInRow[tabIndex] = rect
                                    }
                                },
                        )
                    }
                    // Неполная строка: держим ширину колонок как в остальных.
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Колонок столько, чтобы строки были ровными и в строке было не больше четырёх ячеек. */
private fun chipTabColumns(count: Int): Int {
    if (count <= 0) return 1
    val rows = ceil(count / MAX_COLUMNS.toDouble()).toInt()
    return ceil(count / rows.toDouble()).toInt()
}

private const val MAX_COLUMNS = 4
private const val CONTAINER_ALPHA = 0.34f
private const val SELECTED_CHIP_ALPHA = 0.18f
private const val UNSELECTED_BADGE_ALPHA = 0.18f
private const val SELECTED_BADGE_ALPHA = 0.28f
private val CHIP_CORNER_RADIUS = 6.dp

@Composable
private fun <K> MobileChipTabItem(
    tab: MobileChipTab<K>,
    selected: Boolean,
    selectionFraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = lerp(
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.onSurface,
        selectionFraction,
    )
    val chipShape = RoundedCornerShape(CHIP_CORNER_RADIUS)
    Column(
        modifier = modifier
            .clip(chipShape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tab.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tab.color,
                    modifier = Modifier.size(18.dp),
                )
            }
            tab.count?.let { count ->
                val badgeAlpha = lerpFloat(UNSELECTED_BADGE_ALPHA, SELECTED_BADGE_ALPHA, selectionFraction)
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = tab.color.copy(alpha = badgeAlpha),
                    contentColor = contentColor,
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .heightIn(min = 18.dp)
                            .widthIn(min = 20.dp)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Text(
            text = tab.title,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
