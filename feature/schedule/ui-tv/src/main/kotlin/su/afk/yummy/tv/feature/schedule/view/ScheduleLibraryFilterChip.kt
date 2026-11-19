package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.tv.TvChip
import su.afk.yummy.tv.feature.schedule.presentation.R

/** ТВ-чип переключения фильтра «В библиотеке» на экране расписания. */
@Composable
internal fun ScheduleLibraryFilterChip(
    selected: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvChip(
        label = stringResource(R.string.schedule_filter_in_library),
        selected = selected,
        leadingIcon = Icons.AutoMirrored.Filled.List,
        onClick = onClick,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusProperties {
                leftFocusRequester?.let { left = it }
                downFocusRequester?.let { down = it }
            },
    )
}
