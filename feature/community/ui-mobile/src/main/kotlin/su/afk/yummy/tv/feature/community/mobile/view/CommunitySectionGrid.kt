package su.afk.yummy.tv.feature.community.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.community.mobile.utils.icon
import su.afk.yummy.tv.feature.community.mobile.utils.titleRes
import su.afk.yummy.tv.feature.community.model.CommunitySection

private const val COLUMNS = 4

/** Сетка плиток разделов сообщества: [COLUMNS] в ряд, неполный ряд добивается пустыми ячейками. */
@Composable
internal fun CommunitySectionGrid(
    sections: List<CommunitySection>,
    onSectionSelected: (CommunitySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sections.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { section ->
                    CommunitySectionTile(
                        title = stringResource(section.titleRes()),
                        icon = section.icon(),
                        onClick = { onSectionSelected(section) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
