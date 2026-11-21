package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.details.model.DetailsMinimalTab

/**
 * Ряд текстовых вкладок без плашек. Фокус сам выбирает вкладку, поэтому содержимое под рядом меняется без
 * нажатия. Сфокусированная вкладка подсвечена акцентом, выбранная без фокуса — приглушённо.
 * [tabRequester] — свой FocusRequester у каждой вкладки: вход в ряд всегда на выбранную, а не на первую.
 * [upRequester] — куда уходит «вверх» (кнопки), [downRequester] — первая карточка содержимого.
 */
@Composable
internal fun DetailsMinimalTabRow(
    tabs: List<DetailsMinimalTab>,
    selected: DetailsMinimalTab,
    label: @Composable (DetailsMinimalTab) -> String,
    onSelect: (DetailsMinimalTab) -> Unit,
    tabRequester: (DetailsMinimalTab) -> FocusRequester,
    upRequester: FocusRequester,
    downRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .focusProperties {
                up = upRequester
                down = downRequester
            }
            .focusGroup()
            // Вкладок больше, чем влезает: ряд прокручивается за фокусом.
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 44.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        tabs.forEach { tab ->
            var focused by remember { mutableStateOf(false) }
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .focusRequester(tabRequester(tab))
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onSelect(tab)
                    }
                    .focusable()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = label(tab),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        focused -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(if (isSelected) 40.dp else 0.dp)
                        .background(
                            if (focused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
}
