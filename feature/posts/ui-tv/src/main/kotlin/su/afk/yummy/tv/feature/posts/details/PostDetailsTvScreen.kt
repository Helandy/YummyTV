package su.afk.yummy.tv.feature.posts.details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.core.utils.formatFeedDateTime
import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.feature.posts.details.utils.compactCount
import su.afk.yummy.tv.feature.posts.model.PostContentBlock
import su.afk.yummy.tv.feature.posts.tv.R
import su.afk.yummy.tv.feature.posts.utils.parsePostContent
import su.afk.yummy.tv.feature.posts.view.TvPostFullscreenImageDialog

@Composable
fun PostDetailsTvScreen(
    state: PostDetailsState.State,
    effect: Flow<PostDetailsState.Effect>,
    onEvent: (PostDetailsState.Event) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(effect) {
        effect.collect {
            if (it is PostDetailsState.Effect.ShowToast) Toast.makeText(
                context,
                it.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }
    when {
        state.loading -> TvLoadingScreen()

        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TvStateMessage(
                title = state.error?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.posts_error),
                icon = Icons.Filled.Warning,
                onRetry = { onEvent(PostDetailsState.Event.RetrySelected) },
            )
        }

        state.details != null -> {
            val details = state.details ?: return
            val voting = state.voting
            val context = LocalContext.current
            val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
            val coroutineScope = rememberCoroutineScope()
            val listState = rememberLazyListState()
            val likeFocusRequester = remember { FocusRequester() }
            var fullscreenImage by remember { mutableStateOf<Pair<String, String?>?>(null) }
            val viewsLabel =
                stringResource(R.string.posts_views_short, details.views.compactCount())
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    end = TvScreenPadding.Horizontal,
                    top = TvScreenPadding.Vertical,
                    bottom = TvScreenPadding.Vertical,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    Text(
                        details.category.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val focused by interactionSource.collectIsFocusedAsState()
                    val bringIntoViewRequester = remember { BringIntoViewRequester() }
                    val shape = RoundedCornerShape(12.dp)
                    Text(
                        details.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(
                                if (focused) MaterialTheme.colorScheme.surfaceContainerHigh
                                else Color.Transparent,
                            )
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                                }
                            }
                            .focusable(interactionSource = interactionSource)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                item {
                    TextButton(onClick = { onEvent(PostDetailsState.Event.AuthorSelected(details.author.id)) }) {
                        Text("${details.author.nickname} · ${details.createdAt.formatFeedDateTime()}")
                    }
                }
                details.previewImageUrl?.takeIf(String::isNotBlank)?.let { url ->
                    item {
                        val interactionSource = remember { MutableInteractionSource() }
                        val focused by interactionSource.collectIsFocusedAsState()
                        val bringIntoViewRequester = remember { BringIntoViewRequester() }
                        val shape = RoundedCornerShape(12.dp)
                        AsyncImage(
                            url,
                            details.title,
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 520.dp)
                                .clip(shape)
                                .then(
                                    if (focused) {
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.primary,
                                            shape
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                                    }
                                }
                                .tvFocusableClick(
                                    onClick = { fullscreenImage = url to details.title },
                                    shape = shape,
                                    interactionSource = interactionSource,
                                    focusedScale = 1f,
                                ),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                items(details.contentHtml.parsePostContent(details.previewImageUrl)) { block ->
                    when (block) {
                        is PostContentBlock.Text -> {
                            val interactionSource = remember { MutableInteractionSource() }
                            val focused by interactionSource.collectIsFocusedAsState()
                            val shape = RoundedCornerShape(12.dp)
                            Text(
                                block.value,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .background(
                                        if (focused) MaterialTheme.colorScheme.surfaceContainerHigh
                                        else Color.Transparent,
                                    )
                                    .focusable(interactionSource = interactionSource)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }

                        is PostContentBlock.Image -> {
                            val interactionSource = remember { MutableInteractionSource() }
                            val focused by interactionSource.collectIsFocusedAsState()
                            val bringIntoViewRequester = remember { BringIntoViewRequester() }
                            val shape = RoundedCornerShape(12.dp)
                            AsyncImage(
                                block.url,
                                block.description,
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 620.dp)
                                    .clip(shape)
                                    .then(
                                        if (focused) {
                                            Modifier.border(
                                                3.dp,
                                                MaterialTheme.colorScheme.primary,
                                                shape
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .bringIntoViewRequester(bringIntoViewRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                                        }
                                    }
                                    .tvFocusableClick(
                                        onClick = {
                                            fullscreenImage = block.url to block.description
                                        },
                                        shape = shape,
                                        interactionSource = interactionSource,
                                        focusedScale = 1f,
                                    ),
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                    }
                }
                if (details.relatedAnime.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.posts_related_anime),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    item {
                        val relatedAnimeBringIntoViewRequester =
                            remember { BringIntoViewRequester() }
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(relatedAnimeBringIntoViewRequester),
                            contentPadding = PaddingValues(
                                start = 10.dp,
                                top = 10.dp,
                                end = 10.dp,
                                bottom = 24.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            itemsIndexed(
                                details.relatedAnime,
                                key = { _, anime -> anime.id },
                            ) { index, anime ->
                                val cardShape = RoundedCornerShape(12.dp)
                                Card(
                                    modifier = Modifier
                                        .width(190.dp)
                                        .onFocusChanged {
                                            if (it.isFocused) {
                                                coroutineScope.launch {
                                                    relatedAnimeBringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        }
                                        .focusProperties {
                                            // крайняя левая карточка уводит фокус в боковое меню,
                                            // крайняя правая не даёт фокусу «уехать» в пустоту
                                            if (index == 0) {
                                                mainMenuFocusRequester?.let { left = it }
                                            }
                                            if (index == details.relatedAnime.lastIndex) {
                                                right = FocusRequester.Cancel
                                            }
                                        }
                                        .onPreviewKeyEvent { event ->
                                            if (
                                                event.type == KeyEventType.KeyDown &&
                                                event.key == Key.DirectionDown
                                            ) {
                                                coroutineScope.launch {
                                                    val lastItemIndex =
                                                        listState.layoutInfo.totalItemsCount - 1
                                                    if (lastItemIndex >= 0) {
                                                        listState.scrollToItem(lastItemIndex)
                                                        withFrameNanos { }
                                                    }
                                                    likeFocusRequester.requestFocus()
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        .tvFocusableClick(
                                            onClick = {
                                                onEvent(
                                                    PostDetailsState.Event.AnimeSelected(
                                                        anime.id
                                                    )
                                                )
                                            },
                                            shape = cardShape,
                                        ),
                                    shape = cardShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    ),
                                ) {
                                    anime.posterUrl?.let {
                                        AsyncImage(
                                            it,
                                            anime.title,
                                            Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(.7f),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Column(
                                        Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            anime.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2
                                        )
                                        Text(
                                            listOfNotNull(
                                                anime.year?.toString(),
                                                anime.rating?.let { "★ %.1f".format(it) }).joinToString(
                                                " · "
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        viewsLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PostVoteButton(
                            isLike = true,
                            count = details.reaction.likes,
                            selected = details.reaction.vote == PostVote.LIKE,
                            enabled = !voting,
                            modifier = Modifier.focusRequester(likeFocusRequester),
                            onClick = {
                                if (!voting) onEvent(PostDetailsState.Event.VoteSelected(PostVote.LIKE))
                            },
                        )
                        PostVoteButton(
                            isLike = false,
                            count = details.reaction.dislikes,
                            selected = details.reaction.vote == PostVote.DISLIKE,
                            enabled = !voting,
                            onClick = {
                                if (!voting) onEvent(PostDetailsState.Event.VoteSelected(PostVote.DISLIKE))
                            },
                        )
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.posts_comments_phone_only),
                                    Toast.LENGTH_LONG,
                                ).show()
                            },
                        ) {
                            Text(stringResource(R.string.posts_comments, details.comments))
                        }
                    }
                }
            }
            fullscreenImage?.let { (url, description) ->
                TvPostFullscreenImageDialog(
                    imageUrl = url,
                    contentDescription = description,
                    onDismiss = { fullscreenImage = null },
                )
            }
        }
    }
}

@Composable
private fun PostVoteButton(
    isLike: Boolean,
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val voteColor = if (isLike) YummySemanticColors.Like else YummySemanticColors.Dislike
    val tint = if (selected) voteColor else voteColor.copy(alpha = 0.72f)
    OutlinedButton(
        modifier = modifier.width(112.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(
            imageVector = if (isLike) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 4.dp),
        )
        Text(
            text = count.toString(),
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
