package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.blocksNewDownload
import su.afk.yummy.tv.feature.details.mobile.view.MobileDubbingMeta

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EpisodeDownloadDubbingSheet(
    selection: EpisodesState.EpisodeDownloadDubbingSelection,
    onSelected: (List<AnimeVideo>) -> Unit,
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(
            R.string.details_mobile_download_dubbing_title,
            selection.episode
        ),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp),
    ) {
        if (selection.options.isEmpty()) {
            Text(
                text = stringResource(
                    if (selection.hasAlternativeDubbings) {
                        R.string.details_mobile_download_other_dubbing_empty
                    } else {
                        R.string.details_mobile_download_dubbing_empty
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(selection.options, key = { it.title }) { option ->
                    TextButton(
                        enabled = !option.resolving && !option.status.blocksNewDownload(),
                        onClick = { onSelected(option.videos) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = option.title, modifier = Modifier.fillMaxWidth())
                                // Просмотры и число серий — как в пикере запуска и в плеере.
                                MobileDubbingMeta(
                                    views = option.views,
                                    episodeCount = option.episodeCount,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                                option.subtitle?.let { subtitle ->
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 3.dp),
                                    )
                                }
                            }
                            EpisodeDownloadStatusIcon(option.status, option.resolving)
                        }
                    }
                }
            }
        }
    }
}
