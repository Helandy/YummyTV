package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnail
import su.afk.yummy.tv.core.utils.kodik.resolveContinueWatchingImageModel
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.mobile.utils.timingLabel
import su.afk.yummy.tv.feature.library.mobile.utils.watchedAtLabel
import su.afk.yummy.tv.feature.library.thumbnail.HistoryEpisodeThumbnail

@Composable
internal fun LibraryMobileHistoryPage(
    history: Flow<PagingData<WatchHistoryEntry>>,
    localProgress: ImmutableMap<String, AnimeWatchProgress>,
    isSignedIn: Boolean,
    onEntrySelected: (WatchHistoryEntry) -> Unit,
    onDetailsSelected: (WatchHistoryEntry) -> Unit,
) {
    if (!isSignedIn) {
        HistoryMessage(stringResource(R.string.library_mobile_history_sign_in))
        return
    }
    val items = history.collectAsLazyPagingItems()
    when {
        items.loadState.refresh is LoadState.Loading -> HistoryMessage(null, loading = true)
        items.loadState.refresh is LoadState.Error -> HistoryMessage(stringResource(R.string.library_mobile_history_error))
        items.itemCount == 0 -> HistoryMessage(stringResource(R.string.library_mobile_history_empty))
        else -> LazyColumn(
            modifier = Modifier
                .mobileContentMaxWidth()
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items.itemCount,
                key = { index ->
                    items[index]?.let {
                        lazyKey("history", "${it.animeId}_${it.episode}_${it.watchedAtSeconds}")
                    } ?: lazyKey("history", null, index)
                },
            ) { index ->
                items[index]?.let { entry ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEntrySelected(entry) },
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = entry.screenshotUrl
                                    ?: localProgress["${entry.animeId}:${entry.episode.episodeGroupKey()}"]
                                        ?.let {
                                            resolveContinueWatchingImageModel(
                                                screenshotUrl = it.screenshotUrl,
                                                episodeUrl = it.episodeUrl,
                                                posterUrl = null,
                                                kodikThumbnailModel = ::KodikThumbnail,
                                            )
                                        }
                                    ?: HistoryEpisodeThumbnail(entry.animeId, entry.episode),
                                contentDescription = entry.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(112.dp)
                                    .height(72.dp),
                            ) {
                                val state by painter.state.collectAsStateWithLifecycle()
                                if (state is AsyncImagePainter.State.Success) {
                                    SubcomposeAsyncImageContent()
                                } else {
                                    AsyncImage(
                                        model = entry.posterUrl,
                                        contentDescription = entry.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                                if (entry.episode.isNotBlank()) Text(
                                    stringResource(
                                        R.string.library_mobile_history_episode,
                                        entry.episode
                                    )
                                )
                                entry.timingLabel()?.let { Text(it) }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        entry.watchedAtLabel().orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    ContinueWatchingOverlayButton(
                                        contentDescription = stringResource(
                                            R.string.library_mobile_details_content_description
                                        ),
                                        onClick = { onDetailsSelected(entry) },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (items.loadState.append is LoadState.Loading) item {
                HistoryMessage(
                    null,
                    loading = true
                )
            }
        }
    }
}

@Composable
private fun HistoryMessage(text: String?, loading: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator() else Text(
            text.orEmpty(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
