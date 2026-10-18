package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.common.utils.hasAny
import su.afk.yummy.tv.feature.account.presentation.R

/**
 * Статистика профиля как на мобилке: календарь активности со стриком и страницы
 * статистики (время, списки, жанры, гистограмма оценок) — на ТВ колонкой рядом с календарём.
 */
@Composable
internal fun AccountProfileOverviewPanel(
    summary: UserProfileSummary,
    stats: UserStats?,
    /** Вход в панель сверху — сегодняшний день календаря. */
    activityFocusRequester: FocusRequester? = null,
    topExitFocusRequester: FocusRequester? = null,
    onContentFocusChanged: (Boolean) -> Unit = {},
    onStatsGridExitRight: () -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val statsGridFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                val nextFocused = it.isFocused || it.hasFocus
                if (focused != nextFocused) {
                    focused = nextFocused
                    onContentFocusChanged(nextFocused)
                }
            }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileActivityPanel(
                history = summary.watchHistory,
                daysOnline = summary.daysOnline,
                todayFocusRequester = activityFocusRequester,
                upFocusRequester = topExitFocusRequester,
                rightFocusRequester = statsGridFocusRequester,
                // Отступ сверху совпадает с внутренним отступом сетки под скейл карточек.
                modifier = Modifier
                    .weight(ACTIVITY_WEIGHT)
                    .padding(top = 6.dp),
            )
            ProfileStatsGrid(
                summary = summary,
                stats = stats,
                focusRequester = statsGridFocusRequester,
                topExitFocusRequester = topExitFocusRequester,
                onExitLeft = {
                    activityFocusRequester?.let { runCatching { it.requestFocus() }.isSuccess } ?: false
                },
                onExitRight = onStatsGridExitRight,
                modifier = Modifier.weight(1f - ACTIVITY_WEIGHT),
            )
        }

        if (summary.socialCounts.hasAny()) {
            SurfacePanel(modifier = Modifier.fillMaxWidth()) {
                ProfileSectionHeader(
                    icon = Icons.Filled.Groups,
                    title = stringResource(R.string.account_section_social),
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProfileSocialCounters(counts = summary.socialCounts)
            }
        }
    }
}

private const val ACTIVITY_WEIGHT = 0.4f
