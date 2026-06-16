package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.mobile.model.GenrePickerMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchMobileFilterSheet(
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
    val currentGenrePickerMode = genrePickerMode
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onClose,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (currentGenrePickerMode != null) {
                SearchMobileGenrePickerBody(
                    mode = currentGenrePickerMode,
                    selectedIds = when (currentGenrePickerMode) {
                        GenrePickerMode.INCLUDE -> draftFilters.genres
                        GenrePickerMode.EXCLUDE -> draftFilters.excludedGenres
                    },
                    filterOptions = filterOptions,
                    onGenreToggled = when (currentGenrePickerMode) {
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
                    onReset = onReset,
                    onOpenGenres = { genrePickerMode = GenrePickerMode.INCLUDE },
                    onOpenExcludedGenres = { genrePickerMode = GenrePickerMode.EXCLUDE },
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
            Spacer(Modifier.height(10.dp))
        }
    }
}
