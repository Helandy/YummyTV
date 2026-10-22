package su.afk.yummy.tv.feature.catalog.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.feature.catalog.presentation.R

@Composable
internal fun CatalogTvHeaderRow(
    query: String,
    filters: SearchFilters,
    searchFieldFocusRequester: FocusRequester,
    filterButtonFocusRequester: FocusRequester,
    randomButtonFocusRequester: FocusRequester,
    mainMenuFocusRequester: FocusRequester?,
    /** Куда уходит фокус по «вниз» из строки поиска и кнопок; [FocusRequester.Default], пока результатов нет. */
    downFocusRequester: FocusRequester,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onOpenFilters: () -> Unit,
    isRandomAnimeLoading: Boolean,
    onRandomAnimeSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchEditing by remember { mutableStateOf(false) }
    val clearButtonFocusRequester = remember { FocusRequester() }
    val hasQuery = query.isNotEmpty()
    val searchRightTarget = if (hasQuery) clearButtonFocusRequester else filterButtonFocusRequester

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
                .height(CatalogTvHeaderButtonHeight)
                .focusRequester(searchFieldFocusRequester)
                .focusProperties {
                    mainMenuFocusRequester?.let { left = it }
                    right = searchRightTarget
                    down = downFocusRequester
                }
                .onFocusChanged { if (!it.isFocused) searchEditing = false }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionRight -> {
                            runCatching { searchRightTarget.requestFocus() }
                            true
                        }

                        Key.DirectionDown -> {
                            // Текстовое поле само съедает «вниз», поэтому переводим фокус на результаты вручную
                            if (searchEditing || downFocusRequester == FocusRequester.Default) {
                                false
                            } else {
                                runCatching { downFocusRequester.requestFocus() }.isSuccess
                            }
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
        if (hasQuery) {
            ClearSearchButton(
                onClick = {
                    // Фокус уходит в поле до исчезновения кнопки, иначе Android кинет его в меню
                    runCatching { searchFieldFocusRequester.requestFocus() }
                    onQueryChanged("")
                },
                modifier = Modifier
                    .focusRequester(clearButtonFocusRequester)
                    .focusProperties {
                        left = searchFieldFocusRequester
                        right = filterButtonFocusRequester
                        down = downFocusRequester
                    },
            )
        }
        FilterButton(
            activeCount = filters.activeCount,
            onClick = onOpenFilters,
            modifier = Modifier
                .focusRequester(filterButtonFocusRequester)
                .focusProperties {
                    left = if (hasQuery) clearButtonFocusRequester else searchFieldFocusRequester
                    right = randomButtonFocusRequester
                    down = downFocusRequester
                },
        )
        RandomAnimeButton(
            isLoading = isRandomAnimeLoading,
            onClick = onRandomAnimeSelected,
            modifier = Modifier
                .focusRequester(randomButtonFocusRequester)
                .focusProperties {
                    left = filterButtonFocusRequester
                    down = downFocusRequester
                },
        )
    }
}
