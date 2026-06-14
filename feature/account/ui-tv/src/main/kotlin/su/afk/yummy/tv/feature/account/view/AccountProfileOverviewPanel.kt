package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.utils.hasAny

@Composable
internal fun AccountProfileOverviewPanel(
    summary: UserProfileSummary,
    stats: UserStats?,
    statsGridFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)),
    ) {
        ProfileStatsGrid(
            summary = summary,
            stats = stats,
            focusRequester = statsGridFocusRequester,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DaysOnlineTile(daysOnline = summary.daysOnline)
            ProfileWatchHistoryHeatmap(
                history = summary.watchHistory,
                modifier = Modifier.weight(1f),
            )
        }

        ProfileListCountersRow(
            counts = summary.counts,
            modifier = Modifier.padding(horizontal = 18.dp),
        )

        if (summary.socialCounts.hasAny()) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileSocialCounters(
                counts = summary.socialCounts,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }

        if (summary.about.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = summary.about,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun DaysOnlineTile(daysOnline: Int) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = daysOnline.coerceAtLeast(0).toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = ColorRed,
        )
        Text(
            text = stringResource(R.string.account_profile_days_online),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val ColorRed = androidx.compose.ui.graphics.Color(0xFFFF6B6B)
