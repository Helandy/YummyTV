package su.afk.yummy.tv.feature.schedule.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.feature.schedule.mobile.view.ScheduleMobileReleaseCard
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
internal fun ScheduleDayReleases(
    group: ScheduleDayUi,
    now: ZonedDateTime,
    zone: ZoneId,
    onAnimeSelected: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .mobileContentMaxWidth()
            .fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 10.dp,
            end = 16.dp,
            bottom = MobileBottomBarDefaults.contentBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(group.items, key = { it.focusKey }) { release ->
            ScheduleMobileReleaseCard(
                release = release,
                now = now,
                zone = zone,
                onClick = { onAnimeSelected(release.item.animeId) },
            )
        }
    }
}
