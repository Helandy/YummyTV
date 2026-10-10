package su.afk.yummy.tv.feature.details.full.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.full.utils.formatEpochSeconds
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.formatAiredProgress

/** Индекс ряда жанров в [FullDetailsBody]: всегда после тайтла и (если есть) описания. */
private fun genresItemIndex(details: AnimeDetails): Int? {
    if (details.genres.isEmpty()) return null
    var index = 1
    if (details.description.isNotBlank()) index++
    return index
}

private suspend fun restoreGenreChipFocus(
    listState: LazyListState,
    itemIndex: Int,
    requester: FocusRequester,
) {
    listState.scrollToItem(itemIndex)
    requestFocusUntilTimeout(requester)
}

/** Тянет вниз/вверх список, пока текущий элемент виден не полностью, иначе отдаёт фокус дальше. */
private fun scrollWithinItemKeyEvent(
    scope: CoroutineScope,
    listState: LazyListState,
    itemIndex: Int,
): (KeyEvent) -> Boolean = { event ->
    if (event.type != KeyEventType.KeyDown) {
        false
    } else {
        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == itemIndex }
        val viewportStart = listState.layoutInfo.viewportStartOffset
        val viewportEnd = listState.layoutInfo.viewportEndOffset
        when (event.key) {
            Key.DirectionDown -> {
                val canScrollDown =
                    itemInfo != null && itemInfo.offset + itemInfo.size > viewportEnd
                if (canScrollDown) {
                    scope.launch { listState.animateScrollBy(DescriptionScrollStepPx) }
                }
                canScrollDown
            }

            Key.DirectionUp -> {
                val canScrollUp = itemInfo != null && itemInfo.offset < viewportStart
                if (canScrollUp) {
                    scope.launch { listState.animateScrollBy(-DescriptionScrollStepPx) }
                }
                canScrollUp
            }

            else -> false
        }
    }
}

private const val DescriptionScrollStepPx = 240f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FullDetailsBody(
    details: AnimeDetails,
    onGenreSelected: (Int) -> Unit,
    onStudioSelected: (Int, String?) -> Unit,
    onDirectorSelected: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val firstFocusRequester = remember { FocusRequester() }
    val episodeProgress = details.episodes?.formatAiredProgress()
    var itemIndex = 0

    // Запоминаем чип жанра, с которого ушли на экран жанра, чтобы при возврате назад
    // вернуть фокус именно на него, а не на первый фокусируемый элемент.
    var lastSelectedGenreId by rememberSaveable(details.id) { mutableStateOf<Int?>(null) }
    val genreFocusRequesters = remember(details.genres) {
        details.genres.mapNotNull { it.id }.associateWith { FocusRequester() }
    }

    LaunchedEffect(details.id) {
        val genreIndex = lastSelectedGenreId?.let { genresItemIndex(details) }
        val genreRequester = lastSelectedGenreId?.let { genreFocusRequesters[it] }
        if (genreIndex != null && genreRequester != null) {
            restoreGenreChipFocus(listState, genreIndex, genreRequester)
        } else {
            firstFocusRequester.requestFocus()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        fun nextIndex() = itemIndex++

        item {
            FocusableDetailsItem(
                index = nextIndex(),
                listState = listState,
                firstFocusRequester = firstFocusRequester,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FullRatingRow(details)
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (details.otherTitles.isNotEmpty()) {
                        Text(
                            text = details.otherTitles.joinToString(" | "),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (details.description.isNotBlank()) {
            item {
                val descriptionIndex = itemIndex
                FocusableDetailsItem(
                    index = nextIndex(),
                    listState = listState,
                    onPreviewKeyEvent = scrollWithinItemKeyEvent(
                        scope,
                        listState,
                        descriptionIndex
                    ),
                ) {
                    Text(
                        text = details.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    )
                }
            }
        }

        if (details.genres.isNotEmpty()) {
            item {
                FocusableDetailsItem(
                    index = nextIndex(),
                    listState = listState,
                    focusable = false
                ) {
                    FullDetailsRow(label = stringResource(R.string.details_full_genres)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            details.genres.forEach { genre ->
                                FullDetailsChip(
                                    label = genre.title,
                                    onClick = genre.id?.let { id ->
                                        {
                                            lastSelectedGenreId = id
                                            onGenreSelected(id)
                                        }
                                    },
                                    modifier = genre.id?.let { id ->
                                        genreFocusRequesters[id]?.let { Modifier.focusRequester(it) }
                                    } ?: Modifier,
                                )
                            }
                        }
                    }
                }
            }
        }

        details.type?.let { value ->
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(stringResource(R.string.details_full_type), value)
                }
            }
        }
        details.ageRating?.let { value ->
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(stringResource(R.string.details_full_age_rating), value)
                }
            }
        }
        details.status?.let { value ->
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(stringResource(R.string.details_full_status), value)
                }
            }
        }
        details.year?.let { value ->
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(stringResource(R.string.details_full_year), value.toString())
                }
            }
        }
        if (details.studios.isNotEmpty()) {
            item {
                FocusableDetailsItem(
                    index = nextIndex(),
                    listState = listState,
                    focusable = false
                ) {
                    FullDetailsRow(label = stringResource(R.string.details_relation_studio)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            details.studios.forEach { studio ->
                                FullDetailsChip(
                                    label = studio.title,
                                    onClick = studio.id?.let { id ->
                                        { onStudioSelected(id, studio.url) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (details.creators.isNotEmpty()) {
            item {
                FocusableDetailsItem(
                    index = nextIndex(),
                    listState = listState,
                    focusable = false
                ) {
                    FullDetailsRow(label = stringResource(R.string.details_relation_director)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            details.creators.forEach { creator ->
                                FullDetailsChip(
                                    label = creator.title,
                                    onClick = creator.id?.let { id -> { onDirectorSelected(id) } },
                                )
                            }
                        }
                    }
                }
            }
        }
        episodeProgress?.let {
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(stringResource(R.string.details_full_episodes_progress), it)
                }
            }
        }
        details.episodes?.nextDateEpochSeconds?.let {
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(
                        stringResource(R.string.details_full_next_episode),
                        it.formatEpochSeconds()
                    )
                }
            }
        }
    }
}
