package su.afk.yummy.tv.feature.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.domain.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.view.DetailsError
import su.afk.yummy.tv.feature.details.view.formatRating
import su.afk.yummy.tv.feature.details.view.formatViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FullDetailsTvScreen(
    state: FullDetailsState.State,
    effect: Flow<FullDetailsState.Effect>,
    onEvent: (FullDetailsState.Event) -> Unit,
) {
    val details = state.details
    val error = state.error
    BackHandler { onEvent(FullDetailsState.Event.BackSelected) }
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && details == null -> TvLoadingScreen()
            error != null && details == null -> DetailsError(
                message = error,
                onRetry = { onEvent(FullDetailsState.Event.RetrySelected) },
            )
            details != null -> FullDetailsContent(details)
            else -> TvLoadingScreen()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullDetailsContent(details: AnimeDetails) {
    val listState = rememberLazyListState()
    val firstFocusRequester = remember { FocusRequester() }
    var itemIndex = 0

    LaunchedEffect(details.id) {
        firstFocusRequester.requestFocus()
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
                        style = MaterialTheme.typography.displaySmall,
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
                    if (details.description.isNotBlank()) {
                        Text(
                            text = details.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                    }
                }
            }
        }

        if (details.genres.isNotEmpty()) {
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsRow(label = stringResource(R.string.details_full_genres)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            details.genres.forEach { genre -> FullDetailsChip(genre.title) }
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
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(
                        label = stringResource(R.string.details_full_studio),
                        value = details.studios.joinToString { it.title },
                    )
                }
            }
        }
        if (details.creators.isNotEmpty()) {
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(
                        label = stringResource(R.string.details_full_director),
                        value = details.creators.joinToString { it.title },
                    )
                }
            }
        }
        details.episodes?.aired?.let {
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(stringResource(R.string.details_full_episodes_aired), it.toString())
                }
            }
        }
        details.episodes?.count?.let {
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(stringResource(R.string.details_full_episodes_total), it.toString())
                }
            }
        }
        details.episodes?.nextDateEpochSeconds?.let {
            item {
                FocusableDetailsItem(index = nextIndex(), listState = listState) {
                    FullDetailsTextRow(stringResource(R.string.details_full_next_episode), it.formatEpochSeconds())
                }
            }
        }
    }
}

@Composable
private fun FocusableDetailsItem(
    index: Int,
    listState: LazyListState,
    firstFocusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    scope.launch {
                        listState.animateScrollToItem(index)
                    }
                }
            }
            .focusable()
            .background(
                color = if (isFocused) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        content()
    }
}

@Composable
private fun FullRatingRow(details: AnimeDetails) {
    val ratings = buildList {
        details.rating.average?.let { add(stringResource(R.string.details_full_rating_yani, it.formatRating())) }
        details.views?.let { add(stringResource(R.string.details_full_views, it.formatViews())) }
        details.rating.counters?.let { add(stringResource(R.string.details_full_rating_votes, it)) }
        details.rating.myAnimeList?.let { add("MAL ${it.formatRating()}") }
        details.rating.kinopoisk?.let { add(stringResource(R.string.details_kinopoisk_rating, it.formatRating())) }
        details.rating.shikimori?.let { add("Shikimori ${it.formatRating()}") }
    }
    if (ratings.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ratings.forEach { rating ->
            Text(
                text = rating,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
            )
        }
    }
}

@Composable
private fun FullDetailsTextRow(label: String, value: String) {
    FullDetailsRow(label = label) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
        )
    }
}

@Composable
private fun FullDetailsRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
            modifier = Modifier.width(220.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun FullDetailsChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

private fun Long.formatEpochSeconds(): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(this * 1000))
}
