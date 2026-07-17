package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme

@Composable
internal fun HomeQuickActionsSection(
    title: String,
    scheduleTitle: String,
    reviewsTitle: String,
    showSchedule: Boolean,
    onScheduleClick: () -> Unit,
    onReviewsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MobileSectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showSchedule) {
                HomeQuickActionCard(
                    title = scheduleTitle,
                    icon = Icons.Filled.DateRange,
                    onClick = onScheduleClick,
                    modifier = Modifier.weight(1f),
                )
            }
            HomeQuickActionCard(
                title = reviewsTitle,
                icon = Icons.Filled.RateReview,
                onClick = onReviewsClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeQuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "Schedule and reviews", showBackground = true)
@Composable
private fun HomeQuickActionsSectionPreview() = ScreenPreviewTheme {
    HomeQuickActionsSection(
        title = "Ещё",
        scheduleTitle = "Расписание",
        reviewsTitle = "Рецензии",
        showSchedule = true,
        onScheduleClick = {},
        onReviewsClick = {},
    )
}

@Preview(name = "Reviews only", showBackground = true)
@Composable
private fun HomeQuickActionsReviewsOnlyPreview() = ScreenPreviewTheme {
    HomeQuickActionsSection(
        title = "Ещё",
        scheduleTitle = "Расписание",
        reviewsTitle = "Рецензии",
        showSchedule = false,
        onScheduleClick = {},
        onReviewsClick = {},
    )
}
