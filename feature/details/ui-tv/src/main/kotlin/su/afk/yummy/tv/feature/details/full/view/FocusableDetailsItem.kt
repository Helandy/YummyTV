package su.afk.yummy.tv.feature.details.full.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun FocusableDetailsItem(
    index: Int,
    listState: LazyListState,
    firstFocusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    scope.launch {
                        listState.scrollToItem(index)
                    }
                }
            }
            .focusable()
            .background(
                color = if (isFocused) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        content()
    }
}
