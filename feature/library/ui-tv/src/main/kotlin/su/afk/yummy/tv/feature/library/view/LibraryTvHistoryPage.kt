package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.focus.launchTvLazyListKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnail
import su.afk.yummy.tv.core.utils.kodik.resolveContinueWatchingImageModel
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.feature.library.R
import su.afk.yummy.tv.feature.library.thumbnail.HistoryEpisodeThumbnail
import su.afk.yummy.tv.feature.library.utils.historyFocusKeys
import su.afk.yummy.tv.feature.library.utils.historyProgressKey
import su.afk.yummy.tv.feature.library.utils.timingLabel
import su.afk.yummy.tv.feature.library.utils.watchedAtLabel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LibraryTvHistoryPage(
    history: Flow<PagingData<WatchHistoryEntry>>,
    localProgress: ImmutableMap<String, AnimeWatchProgress>,
    isSignedIn: Boolean,
    gridFocusRequester: FocusRequester,
    focusStateKey: String,
    onEntrySelected: (WatchHistoryEntry) -> Unit,
    onDetailsSelected: (WatchHistoryEntry) -> Unit,
) {
    if (!isSignedIn) {
        HistoryMessage(stringResource(R.string.library_history_sign_in))
        return
    }
    val items = history.collectAsLazyPagingItems()
    // Состояние списка поднято выше when: ветка Loading при возврате на экран не должна сбрасывать скролл.
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val entries = items.itemSnapshotList.items
    val keys = remember(entries) { entries.historyFocusKeys() }
    val itemFocusRequesters = remember(keys) { keys.associateWith { FocusRequester() } }
    val focusRestoreState = rememberTvLazyFocusRestoreState<String>(focusStateKey)
    val restoreTargetFocusRequester = focusRestoreState.savedKey?.let(itemFocusRequesters::get)
        ?: keys.firstOrNull()?.let(itemFocusRequesters::get)
        ?: gridFocusRequester
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }
    var pendingRestoreFocusKey by remember { mutableStateOf<String?>(null) }
    var restoreFocusRequestToken by remember { mutableIntStateOf(0) }
    var handledRestoreFocusRequestToken by remember { mutableIntStateOf(0) }

    fun launchHistoryFocusRestore(): Job {
        pendingRestoreFocusKey = focusRestoreState.savedKey
        return launchTvLazyListKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = keys,
            listState = listState,
            itemFocusRequesters = itemFocusRequesters,
            fallbackFocusRequester = restoreTargetFocusRequester,
            onRestoreFinished = { pendingRestoreFocusKey = null },
        )
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) restoreFocusRequestToken += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Единственный владелец восстановления по возврату на экран: страница, а не LibraryTvScreen.
    // Ждём непустой список — на возврате кэш пагинации доезжает не в первом же кадре.
    LaunchedEffect(keys, restoreFocusRequestToken) {
        if (restoreFocusRequestToken == handledRestoreFocusRequestToken) return@LaunchedEffect
        if (keys.isEmpty()) return@LaunchedEffect
        handledRestoreFocusRequestToken = restoreFocusRequestToken
        if (focusRestoreState.savedKey == null) return@LaunchedEffect
        restoreFocusJob = launchHistoryFocusRestore()
    }

    when {
        items.loadState.refresh is LoadState.Loading -> HistoryMessage(null, true)
        items.loadState.refresh is LoadState.Error -> HistoryMessage(stringResource(R.string.library_history_error))
        items.itemCount == 0 -> HistoryMessage(stringResource(R.string.library_history_empty))
        else -> LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(gridFocusRequester)
                .tvFocusRestorer(
                    fallback = restoreTargetFocusRequester,
                    enabled = keys.isNotEmpty(),
                )
                .focusProperties {
                    onEnter = { restoreFocusJob = launchHistoryFocusRestore() }
                },
            contentPadding = PaddingValues(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items.itemCount,
                // Тот же ключ, что и у восстановления фокуса: иначе сверка по itemInfo.key
                // в launchTvLazyListKeyFocusRestore никогда не совпадает.
                key = { index -> keys.getOrNull(index) ?: lazyKey("history", null, index) },
            ) { index ->
                items[index]?.let { entry ->
                    val entryKey = keys.getOrNull(index)
                    // Реквестер на строку обязателен: без него карточка отрисуется пустой.
                    val fallbackFocusRequester = remember(index) { FocusRequester() }
                    val cardFocusRequester =
                        entryKey?.let(itemFocusRequesters::get) ?: fallbackFocusRequester
                    val detailsFocusRequester = remember(index) { FocusRequester() }

                    fun rememberFocusedCard() {
                        if (entryKey == null) return
                        val restoreKey = pendingRestoreFocusKey
                        // Пока идёт восстановление, промежуточные фокусы не должны затирать цель.
                        // Но если цели в списке уже нет, ждать её бессмысленно.
                        val restoreUnreachable = restoreKey != null && restoreKey !in keys
                        if (restoreKey == null || restoreKey == entryKey || restoreUnreachable) {
                            focusRestoreState.onItemFocused(entryKey, index)
                            pendingRestoreFocusKey = null
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) return@onFocusChanged
                                rememberFocusedCard()
                            }
                            .focusRequester(cardFocusRequester)
                            .focusProperties { right = detailsFocusRequester }
                            .tvFocusableClick(
                                onClick = {
                                    rememberFocusedCard()
                                    onEntrySelected(entry)
                                },
                            ),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SubcomposeAsyncImage(
                                model = entry.screenshotUrl
                                    ?: localProgress[entry.historyProgressKey]
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
                                    .width(160.dp)
                                    .height(90.dp),
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
                                Text(entry.title, style = MaterialTheme.typography.titleLarge)
                                if (entry.episode.isNotBlank()) {
                                    Text(
                                        stringResource(
                                            R.string.library_history_episode,
                                            entry.episode,
                                        ),
                                    )
                                }
                                entry.timingLabel()?.let { Text(it) }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        entry.watchedAtLabel().orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    LibraryDetailsButton(
                                        onClick = {
                                            rememberFocusedCard()
                                            onDetailsSelected(entry)
                                        },
                                        modifier = Modifier
                                            .focusRequester(detailsFocusRequester)
                                            .focusProperties { left = cardFocusRequester },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMessage(text: String?, loading: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = text.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
