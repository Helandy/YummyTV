package su.afk.yummy.tv.feature.details.full.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.full.utils.formatEpochSeconds
import su.afk.yummy.tv.feature.details.view.common.formatAiredProgress

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FullDetailsBody(details: AnimeDetails) {
    val listState = rememberLazyListState()
    val firstFocusRequester = remember { FocusRequester() }
    val episodeProgress = details.episodes?.formatAiredProgress()
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
                    FullDetailsTextRow(stringResource(R.string.details_full_next_episode), it.formatEpochSeconds())
                }
            }
        }
    }
}
