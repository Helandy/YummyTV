package su.afk.yummy.tv.feature.catalog.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.error.uiMessage
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileAppendError
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileSectionLoading
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.catalog.CatalogState
import su.afk.yummy.tv.feature.catalog.mobile.view.CatalogMobileFilterButton
import su.afk.yummy.tv.feature.catalog.mobile.view.CatalogMobileFilterPanel
import su.afk.yummy.tv.feature.catalog.mobile.view.RandomAnimeFloatingButton
import su.afk.yummy.tv.feature.catalog.navigator.CatalogDestination
import su.afk.yummy.tv.feature.catalog.presentation.R

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CatalogMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        CatalogMobileScreen(CatalogDestination(), CatalogState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CatalogMobileScreen(
    dest: CatalogDestination,
    state: CatalogState.State,
    effect: Flow<CatalogState.Effect>,
    onEvent: (CatalogState.Event) -> Unit,
) {
    LaunchedEffect(dest.initialQuery) {
        if (dest.initialQuery.isNotBlank()) {
            onEvent(CatalogState.Event.ExternalSearchSubmitted(dest.initialQuery))
        }
    }

    val results = state.results.collectAsLazyPagingItems()
    val refreshState = results.loadState.refresh
    val appendState = results.loadState.append
    val hasActiveSearch = state.isSearchActive
    val initialError = (refreshState as? LoadState.Error)
        ?.takeIf { results.itemCount == 0 }
        ?.error
        ?.uiMessage()
    val gridState = rememberLazyGridState()
    val keyboardController = LocalSoftwareKeyboardController.current

    BaseScreen(
        isScroll = false,
        error = initialError?.let { ErrorItem(title = it, message = it) },
        onRetry = {
            onEvent(CatalogState.Event.RetrySelected)
            results.retry()
        },
        errorContent = initialError?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.search_mobile_submit),
                    onAction = retry,
                )
            }
        },
        floatingActionButtonEnd = {
            RandomAnimeFloatingButton(
                isLoading = state.isRandomAnimeLoading,
                onClick = { onEvent(CatalogState.Event.RandomAnimeSelected) },
            )
        },
        floatingActionButtonBottomPadding = 8.dp,
    ) {
        MobilePosterGrid(
            contentPadding = PaddingValues(bottom = MobileBottomBarDefaults.contentBottomPadding),
            modifier = Modifier.imePadding(),
            state = gridState,
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "query_field") {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.large,
                    value = state.query,
                    onValueChange = { onEvent(CatalogState.Event.QueryChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        onEvent(CatalogState.Event.SearchSubmitted)
                    }),
                    trailingIcon = {
                        CatalogMobileFilterButton(
                            activeCount = state.filters.activeCount,
                            modifier = Modifier.padding(end = 6.dp),
                            onClick = {
                                keyboardController?.hide()
                                onEvent(CatalogState.Event.OpenFilters)
                            },
                        )
                    },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "submit_button") {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onEvent(CatalogState.Event.SearchSubmitted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.filters.activeCount > 0) {
                            stringResource(
                                R.string.search_mobile_submit_with_filters,
                                state.filters.activeCount,
                            )
                        } else {
                            stringResource(R.string.search_mobile_submit)
                        },
                    )
                }
            }
            if (state.isSearchPending || (hasActiveSearch && refreshState is LoadState.Loading && results.itemCount == 0)) {
                // Лоадер внутри сетки: полноэкранный увёл бы поле поиска из-под пальца при наборе
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                    MobileSectionLoading()
                }
            }
            if (hasActiveSearch && refreshState is LoadState.NotLoading && results.itemCount == 0) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty_state") {
                    Text(
                        text = stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            }
            val error = (appendState as? LoadState.Error)?.error
            if (error != null && results.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "append_error") {
                    MobileAppendError(
                        message = error.uiMessage(),
                        onRetry = { results.retry() },
                    )
                }
            }
            items(
                count = results.itemCount,
                key = results.itemKey { it.id },
                contentType = { "poster" },
            ) { index ->
                results[index]?.let { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        animeId = item.id,
                        rating = item.rating,
                        posterOverlay = {
                            item.year?.takeIf { state.showTitleYear }?.let { year ->
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.inverseOnSurface,
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        },
                        onClick = { onEvent(CatalogState.Event.ItemSelected(item.id)) },
                    )
                }
            }
            if (appendState is LoadState.Loading && results.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "append_loader") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }

    if (state.isFilterPanelOpen) {
        CatalogMobileFilterPanel(
            draftFilters = state.draftFilters,
            filterOptions = state.filterOptions,
            isLoadingFilterOptions = state.isLoadingFilterOptions,
            onClose = { onEvent(CatalogState.Event.ApplyFilters) },
            onReset = { onEvent(CatalogState.Event.ResetDraftFilters) },
            onGenreToggled = { onEvent(CatalogState.Event.GenreToggled(it)) },
            onExcludedGenreToggled = { onEvent(CatalogState.Event.ExcludedGenreToggled(it)) },
            onTypeToggled = { onEvent(CatalogState.Event.TypeToggled(it)) },
            onStatusToggled = { onEvent(CatalogState.Event.StatusToggled(it)) },
            onSeasonToggled = { onEvent(CatalogState.Event.SeasonToggled(it)) },
            onAgeRatingToggled = { onEvent(CatalogState.Event.AgeRatingToggled(it)) },
            onFromYearChanged = { onEvent(CatalogState.Event.FromYearChanged(it)) },
            onToYearChanged = { onEvent(CatalogState.Event.ToYearChanged(it)) },
            onSortSelected = { onEvent(CatalogState.Event.SortSelected(it)) },
            onSortDirectionToggled = { onEvent(CatalogState.Event.SortDirectionToggled) },
        )
    }
}
