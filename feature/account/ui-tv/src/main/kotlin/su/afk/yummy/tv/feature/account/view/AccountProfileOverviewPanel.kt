package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.utils.hasAny

private val PanelHorizontalPadding = 18.dp

@Composable
internal fun AccountProfileOverviewPanel(
    summary: UserProfileSummary,
    stats: UserStats?,
    statsGridFocusRequester: FocusRequester? = null,
    statsGridBottomStartFocusRequester: FocusRequester? = null,
    statsGridTopExitFocusRequester: FocusRequester? = null,
    daysOnlineFocusRequester: FocusRequester? = null,
    listCountersFocusRequester: FocusRequester? = null,
    onContentFocusChanged: (Boolean) -> Unit = {},
    onStatsGridExitRight: () -> Boolean = { false },
    onStatsGridExitDown: () -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

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
            .focusGroup()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)),
    ) {
        ProfileStatsGrid(
            summary = summary,
            stats = stats,
            focusRequester = statsGridFocusRequester,
            bottomStartFocusRequester = statsGridBottomStartFocusRequester,
            topExitFocusRequester = statsGridTopExitFocusRequester,
            onExitRight = onStatsGridExitRight,
            onExitDown = onStatsGridExitDown,
            modifier = Modifier.padding(horizontal = PanelHorizontalPadding, vertical = 16.dp),
        )

        PanelDivider()

        Spacer(modifier = Modifier.height(16.dp))
        ProfileSectionHeader(
            icon = Icons.Filled.Whatshot,
            title = stringResource(R.string.account_tv_section_activity),
            modifier = Modifier.padding(horizontal = PanelHorizontalPadding),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PanelHorizontalPadding, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DaysOnlineTile(
                daysOnline = summary.daysOnline,
                focusRequester = daysOnlineFocusRequester,
                upFocusRequester = statsGridBottomStartFocusRequester ?: statsGridFocusRequester,
                downFocusRequester = listCountersFocusRequester,
            )
            ProfileWatchHistoryHeatmap(
                history = summary.watchHistory,
                modifier = Modifier.weight(1f),
            )
        }

        PanelDivider()

        Spacer(modifier = Modifier.height(16.dp))
        ProfileSectionHeader(
            icon = Icons.Filled.VideoLibrary,
            title = stringResource(R.string.account_tv_section_lists),
            modifier = Modifier.padding(horizontal = PanelHorizontalPadding),
        )
        Spacer(modifier = Modifier.height(12.dp))
        ProfileListCountersRow(
            counts = summary.counts,
            firstFocusRequester = listCountersFocusRequester,
            upFocusRequester = daysOnlineFocusRequester,
            modifier = Modifier.padding(horizontal = PanelHorizontalPadding),
        )

        if (summary.socialCounts.hasAny()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileSectionHeader(
                icon = Icons.Filled.Groups,
                title = stringResource(R.string.account_tv_section_social),
                modifier = Modifier.padding(horizontal = PanelHorizontalPadding),
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileSocialCounters(
                counts = summary.socialCounts,
                modifier = Modifier.padding(horizontal = PanelHorizontalPadding),
            )
        }

        if (summary.about.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileSectionHeader(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.account_tv_section_about),
                modifier = Modifier.padding(horizontal = PanelHorizontalPadding),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.about,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = PanelHorizontalPadding),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun PanelDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = PanelHorizontalPadding),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

@Composable
private fun DaysOnlineTile(
    daysOnline: Int,
    focusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val focusModifier = if (focusRequester != null) {
        Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                upFocusRequester?.let { up = it }
                downFocusRequester?.let { down = it }
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .width(150.dp)
            .then(focusModifier)
            .profileTileVisual(
                focused = focused,
                shape = shape,
                unfocusedContainerAlpha = 0.07f,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Whatshot,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = daysOnline.coerceAtLeast(0).toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.account_profile_days_online_tv),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
