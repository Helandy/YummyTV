package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 */
@Composable
fun <K> MobileChipTabs(
    tabs: List<MobileChipTab<K>>,
    selectedKey: K,
    onSelected: (K) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = chipTabColumns(tabs.size)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            tabs.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { tab ->
                        MobileChipTabItem(
                            tab = tab,
                            selected = tab.key == selectedKey,
                            onClick = { onSelected(tab.key) },
                            modifier = Modifier.weight(1f),
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

@Composable
private fun <K> MobileChipTabItem(
    tab: MobileChipTab<K>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.clickable(role = Role.Tab, onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) tab.color.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
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
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = tab.color.copy(alpha = if (selected) 0.28f else 0.18f),
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
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
