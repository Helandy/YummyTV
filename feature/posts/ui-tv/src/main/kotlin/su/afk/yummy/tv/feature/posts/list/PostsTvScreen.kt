package su.afk.yummy.tv.feature.posts.list

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvAppendErrorFooter
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.feature.posts.tv.R
import su.afk.yummy.tv.feature.posts.utils.label
import su.afk.yummy.tv.feature.posts.view.PostChip
import su.afk.yummy.tv.feature.posts.view.PostTvCard

@Composable
fun PostsTvScreen(
    state: PostsListState.State,
    effect: Flow<PostsListState.Effect>,
    onEvent: (PostsListState.Event) -> Unit
) {
    val posts = state.posts.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current

    val itemCount = posts.itemCount
    val hasContent = posts.loadState.refresh !is LoadState.Loading && itemCount > 0
    val itemIds = remember(posts.itemSnapshotList.items) {
        posts.itemSnapshotList.items.map { it.id }
    }

    // FocusRequester привязан к КАРТОЧКЕ по id поста (не по индексу): при пагинации
    // индексы плывут, и привязка по индексу давала фокус на соседнюю новость.
    // savedFocusedId запоминает, откуда ушли в статью, чтобы вернуться на неё.
    val cardFocusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    var savedFocusedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var isRestoringFocus by remember { mutableStateOf(false) }

    // Цель восстановления фиксируем ОДИН раз при (пере)композиции экрана — до того как
    // фокус, приходящий от скаффолда, сядет на верхнюю видимую карточку и её
    // onFocusChanged перезапишет savedFocusedId.
    val initialRestoreId = remember { savedFocusedId }

    fun cardFocusRequester(id: Int): FocusRequester =
        cardFocusRequesters.getOrPut(id) { FocusRequester() }

    val savedRestoreTargetId = initialRestoreId?.takeIf { itemIds.contains(it) }
    val restoreTargetId = savedRestoreTargetId ?: itemIds.firstOrNull()

    // Регистрируем КОНКРЕТНУЮ карточку как preferred-фокус контента: иначе скаффолд с
    // задержкой фокусирует сам список и уводит фокус на верхнюю видимую карточку,
    // перебивая восстановление.
    DisposableEffect(hasContent, restoreTargetId, registerPreferredContentFocusRequester) {
        val target = restoreTargetId
        if (hasContent && target != null) {
            registerPreferredContentFocusRequester?.invoke(cardFocusRequester(target))
        }
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    // Восстановление фокуса на сохранённой новости (или первой при первом заходе):
    // к сохранённой карточке подкручиваем список, но при первом входе оставляем начало
    // списка с заголовком и фильтрами. Затем ждём появления карточки и просим фокус
    // по кадрам. isRestoringFocus гасит перезапись savedFocusedId промежуточным фокусом.
    LaunchedEffect(hasContent, savedRestoreTargetId) {
        if (!hasContent) return@LaunchedEffect
        val targetId = restoreTargetId ?: return@LaunchedEffect
        isRestoringFocus = true
        savedRestoreTargetId?.let {
            val index = itemIds.indexOf(it)
            listState.scrollToItem((index + HEADER_ITEM_COUNT).coerceAtLeast(0))
        }
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.key == targetId }
        }.first { it }
        withTimeoutOrNull(FOCUS_RESTORE_TIMEOUT_MS) {
            var focused = false
            while (!focused) {
                withFrameNanos { }
                focused = runCatching {
                    cardFocusRequester(targetId).requestFocus()
                }.getOrDefault(false)
            }
        }
        isRestoringFocus = false
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusGroup(),
        contentPadding = PaddingValues(
            start = TvScreenPadding.Horizontal,
            end = TvScreenPadding.Horizontal,
            top = TvScreenPadding.Vertical,
            bottom = TvScreenPadding.Vertical,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.posts_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.weight(1f))
                PostSort.entries.forEach { sort ->
                    PostChip(
                        label = sort.label(),
                        selected = state.sort == sort,
                        onClick = { onEvent(PostsListState.Event.SortSelected(sort)) },
                    )
                }
            }
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            ) {
                item {
                    PostChip(
                        label = stringResource(R.string.posts_all),
                        selected = state.selectedCategory == null,
                        onClick = { onEvent(PostsListState.Event.CategorySelected(null)) },
                    )
                }
                items(state.categories, key = { it.uri }) { category ->
                    PostChip(
                        label = category.title,
                        selected = state.selectedCategory == category.uri,
                        onClick = { onEvent(PostsListState.Event.CategorySelected(category.uri)) },
                    )
                }
            }
        }
        when {
            posts.loadState.refresh is LoadState.Loading -> item {
                Box(
                    Modifier
                        .fillParentMaxHeight(.55f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { TvLoadingScreen() }
            }

            posts.loadState.refresh is LoadState.Error -> item {
                TvStateMessage(
                    title = stringResource(R.string.posts_error),
                    icon = Icons.Filled.Warning,
                    fillMaxSize = false,
                    onRetry = posts::retry,
                )
            }

            posts.itemCount == 0 -> item {
                TvStateMessage(
                    title = stringResource(R.string.posts_empty),
                    icon = Icons.Filled.Article,
                    fillMaxSize = false,
                )
            }

            else -> {
                items(posts.itemCount, key = { index -> posts[index]?.id ?: index }) { index ->
                    posts[index]?.let { post ->
                        PostTvCard(
                            post,
                            onClick = {
                                savedFocusedId = post.id
                                onEvent(PostsListState.Event.PostSelected(post.id))
                            },
                            modifier = Modifier
                                .focusRequester(cardFocusRequester(post.id))
                                .focusProperties {
                                    mainMenuFocusRequester?.let { left = it }
                                }
                                .onFocusChanged {
                                    if (it.hasFocus && !isRestoringFocus) savedFocusedId = post.id
                                },
                        )
                    }
                }
            }
        }
        when (posts.loadState.append) {
            is LoadState.Loading -> item { TvLoadingFooter() }
            is LoadState.Error -> item {
                TvAppendErrorFooter(
                    message = stringResource(R.string.posts_error),
                    onRetry = posts::retry,
                )
            }

            else -> Unit
        }
    }
}

// Ряды-заголовки (заголовок+сортировка, ряд категорий) перед карточками в LazyColumn.
private const val HEADER_ITEM_COUNT = 2
private const val FOCUS_RESTORE_TIMEOUT_MS = 500L
