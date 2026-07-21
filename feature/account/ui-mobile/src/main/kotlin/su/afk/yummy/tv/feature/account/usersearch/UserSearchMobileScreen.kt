package su.afk.yummy.tv.feature.account.usersearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.view.UserSearchCard
import su.afk.yummy.tv.core.designsystem.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchMobileScreen(
    state: UserSearchState.State,
    effect: Flow<UserSearchState.Effect>,
    onEvent: (UserSearchState.Event) -> Unit,
) {
    val results = state.results.collectAsLazyPagingItems()
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    androidx.compose.runtime.LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.user_search_title),
                onBack = { onEvent(UserSearchState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onEvent(UserSearchState.Event.QueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    label = { Text(stringResource(R.string.user_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.PersonSearch, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onEvent(UserSearchState.Event.SearchSubmitted)
                        },
                    ),
                )
            }

            when {
                !state.isSearchActive -> item(key = "initial") {
                    MobileMessage(
                        title = stringResource(R.string.user_search_initial),
                        icon = Icons.Default.PersonSearch,
                        fillMaxSize = false,
                    )
                }

                results.loadState.refresh is LoadState.Loading -> item(key = "loading") {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }

                results.loadState.refresh is LoadState.Error -> item(key = "error") {
                    MobileMessage(
                        title = stringResource(R.string.user_search_error),
                        icon = Icons.Default.PersonSearch,
                        actionLabel = stringResource(CoreR.string.retry),
                        onAction = { results.retry() },
                        fillMaxSize = false,
                    )
                }

                results.itemCount == 0 -> item(key = "empty") {
                    MobileMessage(
                        title = stringResource(R.string.user_search_empty),
                        icon = Icons.Default.PersonSearch,
                        fillMaxSize = false,
                    )
                }

                else -> items(
                    count = results.itemCount,
                    key = { index -> results[index]?.id ?: "placeholder_$index" },
                ) { index ->
                    results[index]?.let { item ->
                        UserSearchCard(
                            item = item,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onEvent(UserSearchState.Event.UserSelected(item.nickname))
                            },
                        )
                    }
                }
            }

            if (results.loadState.append is LoadState.Loading) {
                item(key = "append_loading") { CircularProgressIndicator() }
            }
            if (results.loadState.append is LoadState.Error) {
                item(key = "append_error") {
                    MobileMessage(
                        title = stringResource(R.string.user_search_error),
                        icon = Icons.Default.PersonSearch,
                        actionLabel = stringResource(CoreR.string.retry),
                        onAction = { results.retry() },
                        fillMaxSize = false,
                    )
                }
            }
        }
    }
}
