package su.afk.yummy.tv.feature.schedule.mobile.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.schedule.presentation.R

/** Компактный чип-переключатель фильтра «В библиотеке» для мобильного расписания. */
@Composable
internal fun ScheduleMobileLibraryFilterRow(
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(horizontal = 16.dp)) {
        FilterChip(
            selected = selected,
            onClick = onToggle,
            label = { Text(text = stringResource(R.string.schedule_filter_in_library)) },
            leadingIcon = {
                Icon(
                    imageVector = if (selected) Icons.Filled.Check else Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}
