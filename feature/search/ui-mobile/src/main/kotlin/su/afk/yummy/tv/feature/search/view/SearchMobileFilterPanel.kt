package su.afk.yummy.tv.feature.search.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.mobile.model.GenrePickerMode

/** Filter panel sliding in from the end edge over a dimmed scrim. */
@Composable
internal fun SearchMobileFilterPanel(
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isLoadingFilterOptions: Boolean,
    onClose: () -> Unit,
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
) {
    var genrePickerMode by remember { mutableStateOf<GenrePickerMode?>(null) }
    val visibleState = remember {
        MutableTransitionState(initialState = false).apply { targetState = true }
    }

    fun requestClose() {
        visibleState.targetState = false
    }

    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) onClose()
    }

    BackHandler(enabled = genrePickerMode != null) {
        genrePickerMode = null
    }
    BackHandler(enabled = genrePickerMode == null) {
        requestClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(PANEL_ANIMATION_MILLIS)),
            exit = fadeOut(tween(PANEL_ANIMATION_MILLIS)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = ::requestClose,
                    ),
            )
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInHorizontally(tween(PANEL_ANIMATION_MILLIS)) { it },
            exit = slideOutHorizontally(tween(PANEL_ANIMATION_MILLIS)) { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            val panelShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(PANEL_WIDTH_FRACTION)
                    .clip(panelShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                AnimatedContent(
                    targetState = genrePickerMode,
                    transitionSpec = {
                        val forward = targetState != null
                        slideInHorizontally(tween(PANEL_CONTENT_ANIMATION_MILLIS)) { fullWidth ->
                            if (forward) fullWidth else -fullWidth
                        } + fadeIn(tween(PANEL_CONTENT_ANIMATION_MILLIS)) togetherWith
                                slideOutHorizontally(tween(PANEL_CONTENT_ANIMATION_MILLIS)) { fullWidth ->
                                    if (forward) -fullWidth else fullWidth
                                } + fadeOut(tween(PANEL_CONTENT_ANIMATION_MILLIS))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "filterPanelContent",
                ) { pickerMode ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (pickerMode != null) {
                            SearchMobileGenrePickerBody(
                                mode = pickerMode,
                                selectedIds = when (pickerMode) {
                                    GenrePickerMode.INCLUDE -> draftFilters.genres
                                    GenrePickerMode.EXCLUDE -> draftFilters.excludedGenres
                                },
                                filterOptions = filterOptions,
                                onGenreToggled = when (pickerMode) {
                                    GenrePickerMode.INCLUDE -> onGenreToggled
                                    GenrePickerMode.EXCLUDE -> onExcludedGenreToggled
                                },
                                onBack = { genrePickerMode = null },
                            )
                        } else {
                            SearchMobileFilterBody(
                                draftFilters = draftFilters,
                                filterOptions = filterOptions,
                                isLoadingFilterOptions = isLoadingFilterOptions,
                                onBack = ::requestClose,
                                onReset = onReset,
                                onApply = ::requestClose,
                                onOpenGenres = { genrePickerMode = GenrePickerMode.INCLUDE },
                                onOpenExcludedGenres = {
                                    genrePickerMode = GenrePickerMode.EXCLUDE
                                },
                                onTypeToggled = onTypeToggled,
                                onStatusToggled = onStatusToggled,
                                onSeasonToggled = onSeasonToggled,
                                onAgeRatingToggled = onAgeRatingToggled,
                                onFromYearChanged = onFromYearChanged,
                                onToYearChanged = onToYearChanged,
                                onSortSelected = onSortSelected,
                                onSortDirectionToggled = onSortDirectionToggled,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val PANEL_ANIMATION_MILLIS = 280
private const val PANEL_CONTENT_ANIMATION_MILLIS = 260
private const val PANEL_WIDTH_FRACTION = 0.86f
