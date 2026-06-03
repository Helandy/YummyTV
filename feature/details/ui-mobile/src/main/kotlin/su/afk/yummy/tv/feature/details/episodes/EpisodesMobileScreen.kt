package su.afk.yummy.tv.feature.details.episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.view.DetailsMediaCard
import su.afk.yummy.tv.feature.details.view.DetailsMobileScaffold

@Composable
fun EpisodesMobileScreen(
    state: EpisodesState.State,
    effect: Flow<EpisodesState.Effect>,
    onEvent: (EpisodesState.Event) -> Unit,
) {
    DetailsMobileScaffold(
        title = "Эпизоды",
        onBack = { onEvent(EpisodesState.Event.BackSelected) },
    ) { padding ->
        val content = state.videosState as? VideosUiState.Content
        val episodeGroups = remember(content?.videos) {
            content?.videos.orEmpty().toMobileEpisodeGroups()
        }
        MobileStateContent(
            isLoading = state.videosState is VideosUiState.Loading,
            error = null,
            empty = state.videosState is VideosUiState.Empty,
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(episodeGroups, key = { it.episode }) { group ->
                    EpisodeMobileCard(
                        video = group.video,
                        onClick = { onEvent(EpisodesState.Event.VideoSelected(group.video)) },
                    )
                }
            }
        }
    }

    state.pendingBalancerSelection?.let { picker ->
        BalancerDialog(
            picker = picker,
            onConfirmed = { onEvent(EpisodesState.Event.BalancerConfirmed(it)) },
            onDismiss = { onEvent(EpisodesState.Event.BalancerPickerDismissed) },
        )
    }
}

@Composable
private fun EpisodeMobileCard(
    video: AnimeVideo,
    onClick: () -> Unit,
) {
    val thumbnailUrl by produceState<String?>(null, video.iframeUrl) {
        value = KodikThumbnailExtractor.extract(video.iframeUrl)
    }
    DetailsMediaCard(
        title = "Серия ${video.episode}",
        subtitle = listOf(
            video.dubbing,
            video.player,
            video.durationSeconds?.formatDuration(),
        ).filterNot { it.isNullOrBlank() }.joinToString(" • "),
        imageUrl = thumbnailUrl,
        badge = video.episode,
        onClick = onClick,
    )
}

@Composable
private fun BalancerDialog(
    picker: BalancerPickerState,
    onConfirmed: (AnimeVideo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Серия ${picker.episodeNumber} · Балансер") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                picker.options.forEach { option ->
                    FilledTonalButton(
                        enabled = option.isSupported,
                        onClick = { onConfirmed(option.video) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (option.isSupported) option.playerName else "${option.playerName} · не поддерживается",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}

private data class MobileEpisodeGroup(
    val episode: String,
    val video: AnimeVideo,
)

private fun List<AnimeVideo>.toMobileEpisodeGroups(): List<MobileEpisodeGroup> {
    val bestKodikDubbing = bestKodikDubbing()
    return groupBy { it.episode }
        .entries
        .sortedWith(compareBy({ it.key.toIntOrNull() ?: 0 }, { it.key }))
        .map { (episode, videos) ->
            MobileEpisodeGroup(
                episode = episode,
                video = videos.representativeVideo(bestKodikDubbing),
            )
        }
}

private fun List<AnimeVideo>.bestKodikDubbing(): String {
    val kodikVideos = filter { it.isKodik() }
    return kodikVideos
        .groupBy { it.dubbing }
        .maxByOrNull { (_, videos) -> videos.sumOf { it.views ?: 0 } }
        ?.key
        .orEmpty()
}

private fun List<AnimeVideo>.representativeVideo(bestKodikDubbing: String): AnimeVideo {
    val kodikVideos = filter { it.isKodik() }
    val source = kodikVideos.ifEmpty { this }
    return source.firstOrNull { bestKodikDubbing.isNotBlank() && it.dubbing == bestKodikDubbing }
        ?: source.maxByOrNull { it.views ?: 0 }
        ?: first()
}

private fun AnimeVideo.isKodik(): Boolean =
    player.contains("kodik", ignoreCase = true) ||
        iframeUrl.contains("kodik", ignoreCase = true)

private fun Int.formatDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
