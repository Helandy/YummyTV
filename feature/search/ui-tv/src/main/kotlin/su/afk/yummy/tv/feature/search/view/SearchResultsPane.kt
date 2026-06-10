package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchGenre
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.R
import su.afk.yummy.tv.feature.search.model.GenrePickerMode
import su.afk.yummy.tv.feature.search.utils.ageOptions
import su.afk.yummy.tv.feature.search.utils.label
import su.afk.yummy.tv.feature.search.utils.seasonOptions
import su.afk.yummy.tv.feature.search.utils.statusOptions

@Composable
internal fun SearchResultsPane(
    query: String,
    items: List<SearchItem>,
    isLoading: Boolean,
    canLoadMore: Boolean,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    filters: SearchFilters,
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isFilterPanelOpen: Boolean,
    isLoadingFilterOptions: Boolean,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onItemSelected: (SearchItem) -> Unit,
    onItemFocused: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onOpenFilters: () -> Unit,
    onCloseFilters: () -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
    onGenreToggled: (String) -> Unit,
    onExcludedGenreToggled: (String) -> Unit,
    onTypeToggled: (String) -> Unit,
    onStatusToggled: (String) -> Unit,
    onSeasonToggled: (String) -> Unit,
    onAgeRatingToggled: (Int) -> Unit,
    onFromYearChanged: (Int?) -> Unit,
    onToYearChanged: (Int?) -> Unit,
    onSortSelected: (SearchSort) -> Unit,
    onSortDirectionToggled: () -> Unit,
    restoreFocusedItemOnEnter: Boolean = false,
    onFocusedItemRestoreHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            canLoadMore && total > 0 && lastVisible >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    val focusRequesters = remember(items.size) { List(items.size) { FocusRequester() } }
    val searchFieldFocusRequester = remember { FocusRequester() }
    val filterButtonFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val cardWidth = currentTvTitleCardDimensions().width
    val filterPanelInitialFocusRequester = remember { FocusRequester() }
    val focusedItemIndex = focusedItemId?.let { id -> items.indexOfFirst { it.id == id } } ?: -1
    var lastFocusedIndex by rememberSaveable {
        val idx = focusedItemIndex.coerceAtLeast(0)
        mutableIntStateOf(idx)
    }
    var searchEditing by remember { mutableStateOf(false) }
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var restoreFilterButtonFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var restoreFocusedItemToken by rememberSaveable { mutableIntStateOf(0) }
    val currentRestoreFocusedItemOnEnter by rememberUpdatedState(restoreFocusedItemOnEnter)
    val currentFocusedItemIndex by rememberUpdatedState(focusedItemIndex)

    LaunchedEffect(focusedItemId, items) {
        val focusedIndex = focusedItemId?.let { id -> items.indexOfFirst { it.id == id } }
        if (focusedIndex != null && focusedIndex >= 0) {
            lastFocusedIndex = focusedIndex
        }
    }

    val preferredContentFocusRequester = if (
        restoreFocusedItemOnEnter &&
        focusedItemIndex in items.indices
    ) {
        focusRequesters.getOrNull(focusedItemIndex)
    } else {
        searchFieldFocusRequester
    }

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                currentRestoreFocusedItemOnEnter &&
                currentFocusedItemIndex >= 0
            ) {
                restoreFocusedItemToken += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(restoreFocusedItemToken, focusedItemIndex, items, focusRequesters) {
        if (
            restoreFocusedItemToken <= 0 ||
            !restoreFocusedItemOnEnter ||
            focusedItemIndex !in items.indices
        ) {
            return@LaunchedEffect
        }
        isRestoringFocus = true
        gridState.scrollToItem(focusedItemIndex)
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.any { it.index == focusedItemIndex }
        }.first { it }
        repeat(6) {
            runCatching { focusRequesters[focusedItemIndex].requestFocus() }
            withFrameNanos { }
        }
        isRestoringFocus = false
        onFocusedItemRestoreHandled()
    }

    LaunchedEffect(restoreFilterButtonFocusToken, isFilterPanelOpen) {
        if (restoreFilterButtonFocusToken <= 0 || isFilterPanelOpen) return@LaunchedEffect
        repeat(6) {
            runCatching { filterButtonFocusRequester.requestFocus() }
            withFrameNanos { }
        }
    }

    // Lift the focused card's row to the top once focus settles. A cancellable
    // effect keeps the focused row pinned so the row below stays composed and
    // DPAD-down preserves the column.
    LaunchedEffect(lastFocusedIndex, gridHasFocus) {
        if (gridHasFocus && !isRestoringFocus && items.isNotEmpty()) {
            gridState.animateScrollToItem(lastFocusedIndex.coerceIn(0, items.lastIndex))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TvScreenPadding.Horizontal, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                readOnly = !searchEditing,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    searchEditing = false
                    onSearchSubmitted()
                }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFieldFocusRequester)
                    .focusProperties {
                        mainMenuFocusRequester?.let { left = it }
                        right = filterButtonFocusRequester
                    }
                    .onFocusChanged { if (!it.isFocused) searchEditing = false }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionRight -> {
                                runCatching { filterButtonFocusRequester.requestFocus() }
                                true
                            }

                            Key.DirectionCenter,
                            Key.Enter,
                            Key.NumPadEnter,
                                -> {
                                if (!searchEditing) {
                                    searchEditing = true
                                    keyboardController?.show()
                                    true
                                } else {
                                    false
                                }
                            }

                            Key.Back -> {
                                if (searchEditing) {
                                    keyboardController?.hide()
                                    searchEditing = false
                                    true
                                } else {
                                    false
                                }
                            }

                            else -> false
                        }
                    },
            )
            FilterButton(
                activeCount = filters.activeCount,
                onClick = onOpenFilters,
                modifier = Modifier
                    .focusRequester(filterButtonFocusRequester)
                    .focusProperties { left = searchFieldFocusRequester },
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isFilterPanelOpen -> FilterPanel(
                    draftFilters = draftFilters,
                    filterOptions = filterOptions,
                    isLoadingFilterOptions = isLoadingFilterOptions,
                    initialFocusRequester = filterPanelInitialFocusRequester,
                    onClose = {
                        restoreFilterButtonFocusToken += 1
                        onCloseFilters()
                    },
                    onApply = {
                        restoreFilterButtonFocusToken += 1
                        onApplyFilters()
                    },
                    onReset = onResetFilters,
                    onGenreToggled = onGenreToggled,
                    onExcludedGenreToggled = onExcludedGenreToggled,
                    onTypeToggled = onTypeToggled,
                    onStatusToggled = onStatusToggled,
                    onSeasonToggled = onSeasonToggled,
                    onAgeRatingToggled = onAgeRatingToggled,
                    onFromYearChanged = onFromYearChanged,
                    onToYearChanged = onToYearChanged,
                    onSortSelected = onSortSelected,
                    onSortDirectionToggled = onSortDirectionToggled,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                items.isEmpty() && isLoading -> TvLoadingScreen()
                items.isEmpty() && (query.isNotBlank() || !filters.isEmpty) -> {
                    Text(
                        text = stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val horizontalSpacing = TvCardSpacing.Horizontal
                        val gridColumnCount =
                            (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                                    (cardWidth.value + horizontalSpacing.value)).toInt()
                                .coerceAtLeast(1)
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = cardWidth),
                            contentPadding = PaddingValues(
                                start = TvScreenPadding.Horizontal,
                                end = TvScreenPadding.Horizontal,
                                top = 8.dp,
                                bottom = TvScreenPadding.Vertical,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                            verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                            modifier = Modifier
                                .fillMaxSize()
                                .onFocusChanged { state ->
                                    val hadFocus = gridHasFocus
                                    gridHasFocus = state.hasFocus
                                    if (!state.hasFocus) {
                                        isRestoringFocus = false
                                    }
                                    if (state.hasFocus && !hadFocus && items.isNotEmpty()) {
                                        isRestoringFocus = true
                                        scope.launch {
                                            val focusedIndex = focusedItemId?.let { id ->
                                                items.indexOfFirst { it.id == id }
                                            }?.takeIf { it >= 0 }
                                            val target = focusedIndex ?: lastFocusedIndex.coerceIn(
                                                0,
                                                items.lastIndex
                                            )
                                            gridState.scrollToItem(target)
                                            snapshotFlow {
                                                gridState.layoutInfo.visibleItemsInfo.any { it.index == target }
                                            }.first { it }
                                            runCatching { focusRequesters[target].requestFocus() }
                                            isRestoringFocus = false
                                            if (restoreFocusedItemOnEnter) {
                                                onFocusedItemRestoreHandled()
                                            }
                                        }
                                    }
                                }
                                .focusGroup(),
                        ) {
                            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                                val stableOnClick = remember(item.id, index) {
                                    {
                                        lastFocusedIndex = index
                                        onItemFocused(item.id)
                                        onItemSelected(item)
                                    }
                                }
                                val stableOnFocused =
                                    remember(item.id) { { onItemFocused(item.id) } }
                                TvTitleCard(
                                    title = item.title,
                                    posterUrl = item.posterUrl,
                                    onClick = stableOnClick,
                                    screenshotUrls = if (item.id == focusedItemId) focusedPreview?.screenshotUrls.orEmpty() else emptyList(),
                                    onFocused = stableOnFocused,
                                    modifier = Modifier
                                        .focusRequester(focusRequesters[index])
                                        .onPreviewKeyEvent { event ->
                                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                            if (event.key != Key.DirectionLeft) return@onPreviewKeyEvent false
                                            if (index % gridColumnCount != 0) return@onPreviewKeyEvent false
                                            runCatching { mainMenuFocusRequester?.requestFocus() }
                                            mainMenuFocusRequester != null
                                        }
                                        .onFocusChanged { state ->
                                            if (state.hasFocus) {
                                                if (!isRestoringFocus) {
                                                    lastFocusedIndex = index
                                                }
                                                if (restoreFocusedItemOnEnter && item.id == focusedItemId) {
                                                    onFocusedItemRestoreHandled()
                                                }
                                            }
                                        },
                                    posterOverlay = item.rating?.let { rating ->
                                        {
                                            Text(
                                                text = "%.2f".format(rating),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                            )
                                        }
                                    },
                                )
                            }
                            if (isLoading && items.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        TvLoadingFooter()
                                    }
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
private fun FilterButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (activeCount > 0) {
        stringResource(R.string.search_filters_with_count, activeCount)
    } else {
        stringResource(R.string.search_filters)
    }
    SelectableRow(
        label = label,
        selected = activeCount > 0,
        onClick = onClick,
        modifier = modifier.widthIn(min = 148.dp),
    )
}

@Composable
private fun FilterPanel(
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isLoadingFilterOptions: Boolean,
    initialFocusRequester: FocusRequester,
    onClose: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onGenreToggled: (String) -> Unit,
    onExcludedGenreToggled: (String) -> Unit,
    onTypeToggled: (String) -> Unit,
    onStatusToggled: (String) -> Unit,
    onSeasonToggled: (String) -> Unit,
    onAgeRatingToggled: (Int) -> Unit,
    onFromYearChanged: (Int?) -> Unit,
    onToYearChanged: (Int?) -> Unit,
    onSortSelected: (SearchSort) -> Unit,
    onSortDirectionToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var genrePickerMode by remember { mutableStateOf<GenrePickerMode?>(null) }
    var restoreGenreButtonMode by remember { mutableStateOf<GenrePickerMode?>(null) }
    val currentGenrePickerMode = genrePickerMode
    val includeGenresFocusRequester = remember { FocusRequester() }
    val excludeGenresFocusRequester = remember { FocusRequester() }
    val genreBackFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        repeat(6) {
            runCatching { initialFocusRequester.requestFocus() }
            withFrameNanos { }
        }
    }

    LaunchedEffect(currentGenrePickerMode, restoreGenreButtonMode) {
        val pickerMode = currentGenrePickerMode
        if (pickerMode != null) {
            runCatching { genreBackFocusRequester.requestFocus() }
            return@LaunchedEffect
        }

        when (restoreGenreButtonMode) {
            GenrePickerMode.INCLUDE -> runCatching { includeGenresFocusRequester.requestFocus() }
            GenrePickerMode.EXCLUDE -> runCatching { excludeGenresFocusRequester.requestFocus() }
            null -> Unit
        }
        restoreGenreButtonMode = null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 920.dp)
            .padding(horizontal = TvScreenPadding.Horizontal, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.Back) {
                    return@onPreviewKeyEvent false
                }
                if (currentGenrePickerMode != null) {
                    restoreGenreButtonMode = currentGenrePickerMode
                    genrePickerMode = null
                } else {
                    onClose()
                }
                true
            }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (currentGenrePickerMode != null) {
            GenrePickerScreen(
                title = when (currentGenrePickerMode) {
                    GenrePickerMode.INCLUDE -> stringResource(R.string.search_filter_genres)
                    GenrePickerMode.EXCLUDE -> stringResource(R.string.search_filter_exclude_genres)
                },
                selectedIds = when (currentGenrePickerMode) {
                    GenrePickerMode.INCLUDE -> draftFilters.genres
                    GenrePickerMode.EXCLUDE -> draftFilters.excludedGenres
                },
                filterOptions = filterOptions,
                onGenreToggled = when (currentGenrePickerMode) {
                    GenrePickerMode.INCLUDE -> onGenreToggled
                    GenrePickerMode.EXCLUDE -> onExcludedGenreToggled
                },
                backFocusRequester = genreBackFocusRequester,
                onBack = {
                    restoreGenreButtonMode = currentGenrePickerMode
                    genrePickerMode = null
                },
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_filters),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelectableRow(
                    label = stringResource(R.string.search_filters_apply),
                    selected = true,
                    onClick = onApply,
                    modifier = Modifier.focusRequester(initialFocusRequester),
                )
                SelectableRow(
                    label = stringResource(R.string.search_filters_reset),
                    selected = false,
                    onClick = onReset,
                )
                SelectableRow(
                    label = stringResource(R.string.search_filters_close),
                    selected = false,
                    onClick = onClose,
                )
            }
        }

        if (isLoadingFilterOptions) {
            Text(
                text = stringResource(R.string.search_filters_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FilterSection(title = stringResource(R.string.search_filter_sort)) {
            ChipFlow {
                SearchSort.entries.forEach { sort ->
                    SelectableRow(
                        label = sort.label(),
                        selected = draftFilters.sort == sort,
                        onClick = { onSortSelected(sort) },
                    )
                }
                SelectableRow(
                    label = if (draftFilters.sortForward) {
                        stringResource(R.string.search_filter_sort_forward)
                    } else {
                        stringResource(R.string.search_filter_sort_backward)
                    },
                    selected = !draftFilters.sortForward,
                    onClick = onSortDirectionToggled,
                )
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_year)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                YearField(
                    label = stringResource(R.string.search_filter_year_from),
                    value = draftFilters.fromYear,
                    onValueChanged = onFromYearChanged,
                    modifier = Modifier.weight(1f),
                )
                YearField(
                    label = stringResource(R.string.search_filter_year_to),
                    value = draftFilters.toYear,
                    onValueChanged = onToYearChanged,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_type)) {
            ChipFlow {
                filterOptions.types.forEach { type ->
                    SelectableRow(
                        label = type.title,
                        selected = type.id in draftFilters.types,
                        onClick = { onTypeToggled(type.id) },
                    )
                }
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_status)) {
            ChipFlow {
                statusOptions().forEach { option ->
                    SelectableRow(
                        label = option.label,
                        selected = option.value in draftFilters.statuses,
                        onClick = { onStatusToggled(option.value) },
                    )
                }
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_season)) {
            ChipFlow {
                seasonOptions().forEach { option ->
                    SelectableRow(
                        label = option.label,
                        selected = option.value in draftFilters.seasons,
                        onClick = { onSeasonToggled(option.value) },
                    )
                }
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_age)) {
            ChipFlow {
                ageOptions().forEach { option ->
                    SelectableRow(
                        label = option.label,
                        selected = option.value in draftFilters.ageRatings,
                        onClick = { onAgeRatingToggled(option.value) },
                    )
                }
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_genre_screen_title)) {
            ChipFlow {
                SelectableRow(
                    label = stringResource(R.string.search_filter_open_genres, draftFilters.genres.size),
                    selected = draftFilters.genres.isNotEmpty(),
                    onClick = { genrePickerMode = GenrePickerMode.INCLUDE },
                    modifier = Modifier.focusRequester(includeGenresFocusRequester),
                )
                SelectableRow(
                    label = stringResource(R.string.search_filter_open_exclude_genres, draftFilters.excludedGenres.size),
                    selected = draftFilters.excludedGenres.isNotEmpty(),
                    onClick = { genrePickerMode = GenrePickerMode.EXCLUDE },
                    modifier = Modifier.focusRequester(excludeGenresFocusRequester),
                )
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}

@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun GenresSection(
    title: String,
    selectedIds: Set<String>,
    filterOptions: SearchFilterOptions,
    onGenreToggled: (String) -> Unit,
) {
    FilterSection(title = title) {
        val genresByGroup = filterOptions.genres.groupBy { it.groupId }
        filterOptions.genreGroups.forEach { group ->
            val genres = genresByGroup[group.id].orEmpty()
            if (genres.isNotEmpty()) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                ChipFlow {
                    genres.forEach { genre ->
                        GenreRow(
                            genre = genre,
                            selected = genre.id in selectedIds,
                            onClick = { onGenreToggled(genre.id) },
                        )
                    }
                }
            }
        }
        val ungroupedGenres = filterOptions.genres
            .filter { genre -> filterOptions.genreGroups.none { it.id == genre.groupId } }
        if (ungroupedGenres.isNotEmpty()) {
            ChipFlow {
                ungroupedGenres.forEach { genre ->
                    GenreRow(
                        genre = genre,
                        selected = genre.id in selectedIds,
                        onClick = { onGenreToggled(genre.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GenrePickerScreen(
    title: String,
    selectedIds: Set<String>,
    filterOptions: SearchFilterOptions,
    onGenreToggled: (String) -> Unit,
    backFocusRequester: FocusRequester,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.search_filter_selected_count, selectedIds.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SelectableRow(
            label = stringResource(R.string.search_filters_back),
            selected = false,
            onClick = onBack,
            modifier = Modifier.focusRequester(backFocusRequester),
        )
    }
    GenresSection(
        title = stringResource(R.string.search_filter_genre_screen_title),
        selectedIds = selectedIds,
        filterOptions = filterOptions,
        onGenreToggled = onGenreToggled,
    )
}

@Composable
private fun GenreRow(
    genre: SearchGenre,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectableRow(
        label = genre.title,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun YearField(
    label: String,
    value: Int?,
    onValueChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { text ->
            onValueChanged(text.filter { it.isDigit() }.take(4).toIntOrNull())
        },
        label = { Text(label) },
        readOnly = !editing,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier
            .onFocusChanged { if (!it.isFocused) editing = false }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter,
                    Key.Enter,
                    Key.NumPadEnter,
                        -> {
                        if (!editing) {
                            editing = true
                            keyboardController?.show()
                            true
                        } else {
                            false
                        }
                    }

                    Key.Back -> {
                        if (editing) {
                            keyboardController?.hide()
                            editing = false
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            },
    )
}

@Composable
private fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val borderColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val backgroundColor = when {
        selected && focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        focused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .clip(shape)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .background(backgroundColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) stringResource(R.string.search_filter_selected, label) else label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
    }
}
