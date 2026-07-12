package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.feature.search.R

@Composable
internal fun SearchTvHeaderRow(
    query: String,
    filters: SearchFilters,
    searchFieldFocusRequester: FocusRequester,
    filterButtonFocusRequester: FocusRequester,
    randomButtonFocusRequester: FocusRequester,
    mainMenuFocusRequester: FocusRequester?,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onOpenFilters: () -> Unit,
    isRandomAnimeLoading: Boolean,
    onRandomAnimeSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchEditing by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
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
                .focusProperties {
                    left = searchFieldFocusRequester
                    right = randomButtonFocusRequester
                },
        )
        RandomAnimeButton(
            isLoading = isRandomAnimeLoading,
            onClick = onRandomAnimeSelected,
            modifier = Modifier
                .focusRequester(randomButtonFocusRequester)
                .focusProperties { left = filterButtonFocusRequester },
        )
    }
}
