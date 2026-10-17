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
import androidx.compose.runtime.CompositionLocalProvider
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
import su.afk.yummy.tv.feature.details.full.model.FullDetailsFocusOrder
import su.afk.yummy.tv.feature.details.full.model.LocalFullDetailsFocusOrder
import su.afk.yummy.tv.feature.details.full.utils.formatEpochSeconds
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.formatAiredProgress

private const val GenreChipPrefix = "genre:"
private const val StudioChipPrefix = "studio:"
private const val CreatorChipPrefix = "creator:"

/**
 * Индекс ряда чипов в [FullDetailsBody] по ключу чипа. Порядок рядов тот же, что в [LazyColumn]:
 * тайтл, описание, жанры, тип, возраст, статус, год, студии, режиссёры.
 */
private fun chipRowIndex(details: AnimeDetails, chipKey: String): Int? {
    var index = 1
    if (details.description.isNotBlank()) index++
    if (chipKey.startsWith(GenreChipPrefix)) return index.takeIf { details.genres.isNotEmpty() }
    if (details.genres.isNotEmpty()) index++
    listOf(details.type, details.ageRating, details.status, details.year).forEach { if (it != null) index++ }
    if (chipKey.startsWith(StudioChipPrefix)) return index.takeIf { details.studios.isNotEmpty() }
    if (details.studios.isNotEmpty()) index++
    if (chipKey.startsWith(CreatorChipPrefix)) return index.takeIf { details.creators.isNotEmpty() }
    return null
}

private suspend fun restoreChipFocus(
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
    val focusOrder = remember { FullDetailsFocusOrder(firstFocusRequester) }
    val episodeProgress = details.episodes?.formatAiredProgress()

    // Запоминаем чип, с которого ушли на экран жанра/студии/режиссёра, чтобы при возврате назад
    // вернуть фокус именно на него, а не на первый фокусируемый элемент.
    var lastSelectedChip by rememberSaveable(details.id) { mutableStateOf<String?>(null) }
    val chipFocusRequesters = remember(details.genres, details.studios, details.creators) {
        buildMap {
            details.genres.forEach { g -> g.id?.let { put("$GenreChipPrefix$it", FocusRequester()) } }
            details.studios.forEach { st -> st.id?.let { put("$StudioChipPrefix$it", FocusRequester()) } }
            details.creators.forEach { c -> c.id?.let { put("$CreatorChipPrefix$it", FocusRequester()) } }
        }
    }

    LaunchedEffect(details.id) {
        val chipIndex = lastSelectedChip?.let { chipRowIndex(details, it) }
        val chipRequester = lastSelectedChip?.let { chipFocusRequesters[it] }
        if (chipIndex != null && chipRequester != null) {
            restoreChipFocus(listState, chipIndex, chipRequester)
        } else {
            firstFocusRequester.requestFocus()
        }
    }

    CompositionLocalProvider(LocalFullDetailsFocusOrder provides focusOrder) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Счётчик живёт внутри билдера: лямбда перезапускается, и внешний счётчик
            // уезжал бы дальше, сдвигая индексы рядов.
            var itemIndex = 0
            fun nextIndex() = itemIndex++

            val rowIndex0 = nextIndex()
            item {
                FocusableDetailsItem(
                    index = rowIndex0,
                    listState = listState,
                    firstFocusRequester = firstFocusRequester,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FullRatingRow(details)
                        Text(
                            text = details.title,
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                            fontWeight = FontWeight.SemiBold,
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
                val rowIndex1 = nextIndex()
                item {
                    FocusableDetailsItem(
                        index = rowIndex1,
                        listState = listState,
                        onPreviewKeyEvent = scrollWithinItemKeyEvent(
                            scope,
                            listState,
                            rowIndex1,
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
                val rowIndex2 = nextIndex()
                item {
                    FocusableDetailsItem(
                        index = rowIndex2,
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
                                                lastSelectedChip = "$GenreChipPrefix$id"
                                                onGenreSelected(id)
                                            }
                                        },
                                        modifier = genre.id?.let { id ->
                                            chipFocusRequesters["$GenreChipPrefix$id"]
                                                ?.let { Modifier.focusRequester(it) }
                                        } ?: Modifier,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            details.type?.let { value ->
                val rowIndex3 = nextIndex()
                item {
                    FocusableDetailsItem(index = rowIndex3, listState = listState) {
                        FullDetailsTextRow(stringResource(R.string.details_full_type), value)
                    }
                }
            }
            details.ageRating?.let { value ->
                val rowIndex4 = nextIndex()
                item {
                    FocusableDetailsItem(index = rowIndex4, listState = listState) {
                        FullDetailsTextRow(stringResource(R.string.details_full_age_rating), value)
                    }
                }
            }
            details.status?.let { value ->
                val rowIndex5 = nextIndex()
                item {
                    FocusableDetailsItem(index = rowIndex5, listState = listState) {
                        FullDetailsTextRow(stringResource(R.string.details_full_status), value)
                    }
                }
            }
            details.year?.let { value ->
                val rowIndex6 = nextIndex()
                item {
                    FocusableDetailsItem(index = rowIndex6, listState = listState) {
                        FullDetailsTextRow(stringResource(R.string.details_full_year), value.toString())
                    }
                }
            }
            if (details.studios.isNotEmpty()) {
                val rowIndex7 = nextIndex()
                item {
                    FocusableDetailsItem(
                        index = rowIndex7,
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
                                            {
                                                lastSelectedChip = "$StudioChipPrefix$id"
                                                onStudioSelected(id, studio.url)
                                            }
                                        },
                                        modifier = studio.id?.let { id ->
                                            chipFocusRequesters["$StudioChipPrefix$id"]
                                                ?.let { Modifier.focusRequester(it) }
                                        } ?: Modifier,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (details.creators.isNotEmpty()) {
                val rowIndex8 = nextIndex()
                item {
                    FocusableDetailsItem(
                        index = rowIndex8,
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
                                        onClick = creator.id?.let { id ->
                                            {
                                                lastSelectedChip = "$CreatorChipPrefix$id"
                                                onDirectorSelected(id)
                                            }
                                        },
                                        modifier = creator.id?.let { id ->
                                            chipFocusRequesters["$CreatorChipPrefix$id"]
                                                ?.let { Modifier.focusRequester(it) }
                                        } ?: Modifier,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            episodeProgress?.let {
                val rowIndex9 = nextIndex()
                item {
                    FocusableDetailsItem(index = rowIndex9, listState = listState) {
                        FullDetailsTextRow(stringResource(R.string.details_full_episodes_progress), it)
                    }
                }
            }
            details.episodes?.nextDateEpochSeconds?.let {
                val rowIndex10 = nextIndex()
                item {
                    FocusableDetailsItem(index = rowIndex10, listState = listState) {
                        FullDetailsTextRow(
                            stringResource(R.string.details_full_next_episode),
                            it.formatEpochSeconds()
                        )
                    }
                }
            }
        }
    }
}
