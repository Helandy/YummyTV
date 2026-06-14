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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.utils.formatProfileDate
import su.afk.yummy.tv.feature.account.utils.hasAny
import su.afk.yummy.tv.feature.account.utils.label
import su.afk.yummy.tv.feature.account.utils.toProfileHoursLabel
import su.afk.yummy.tv.feature.account.utils.totalWatchSeconds
import su.afk.yummy.tv.feature.account.utils.watchSlices

@Composable
internal fun AccountProfileOverviewPanel(
    summary: UserProfileSummary,
    fallbackNickname: String,
    fallbackAvatarUrl: String,
    modifier: Modifier = Modifier,
) {
    val nickname = summary.nickname.ifBlank { fallbackNickname }
    val avatarUrl = summary.avatarUrl ?: fallbackAvatarUrl
    val slices = summary.watchSlices()
    val totalWatchSeconds = summary.totalWatchSeconds()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AccountAvatar(avatarUrl = avatarUrl.orEmpty(), nickname = nickname)
            ProfileMetadata(
                summary = summary,
                nickname = nickname,
                modifier = Modifier.weight(0.9f)
            )
            if (slices.isNotEmpty()) {
                Column(
                    modifier = Modifier.weight(1.8f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.account_profile_watch_time_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    ProfileWatchLegend(slices = slices)
                }
                ProfileWatchPieChart(
                    slices = slices,
                    totalLabel = totalWatchSeconds.toProfileHoursLabel(),
                    percentLabel = stringResource(R.string.account_profile_percent_full),
                )
            }
        }

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
private fun ProfileMetadata(
    summary: UserProfileSummary,
    nickname: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = nickname,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ProfileMetaLine(
            label = stringResource(R.string.account_profile_registered),
            value = summary.registerDateSeconds.formatProfileDate(),
        )
        ProfileMetaLine(
            label = stringResource(R.string.account_profile_birth_date),
            value = summary.birthDateSeconds.formatProfileDate(),
        )
        ProfileMetaLine(
            label = stringResource(R.string.account_profile_sex),
            value = summary.sex.label(),
        )
    }
}

@Composable
private fun ProfileMetaLine(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        text = stringResource(R.string.account_profile_meta_line, label, value),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
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
