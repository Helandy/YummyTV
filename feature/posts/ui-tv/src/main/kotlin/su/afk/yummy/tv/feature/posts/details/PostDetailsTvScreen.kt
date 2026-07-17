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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.core.utils.formatFeedDateTime
import su.afk.yummy.tv.domain.posts.model.PostDetails
import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.feature.posts.model.PostContentBlock
import su.afk.yummy.tv.feature.posts.tv.R
import su.afk.yummy.tv.feature.posts.utils.parsePostContent
import java.util.Locale

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

        state.details != null -> state.details?.let {
            PostDetailsContent(
                it,
                state.voting,
                onEvent
            )
        }
    }
}

@Composable
private fun PostDetailsContent(
    details: PostDetails,
    voting: Boolean,
    onEvent: (PostDetailsState.Event) -> Unit
) {
    val context = LocalContext.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val viewsLabel = stringResource(R.string.posts_views_short, details.views.compactCount())
    LazyColumn(
        Modifier.fillMaxSize(),
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
            Text(
                details.title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        item {
            TextButton(onClick = { onEvent(PostDetailsState.Event.AuthorSelected(details.author.id)) }) {
                Text("${details.author.nickname} · ${details.createdAt.formatFeedDateTime()}")
            }
        }
        details.previewImageUrl?.takeIf(String::isNotBlank)?.let { url ->
            item {
                AsyncImage(
                    url,
                    details.title,
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
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
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                                } else {
                                    Modifier
                                },
                            )
                            .focusable(interactionSource = interactionSource),
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    itemsIndexed(
                        details.relatedAnime,
                        key = { _, anime -> anime.id },
                    ) { index, anime ->
                        val cardShape = RoundedCornerShape(12.dp)
                        Card(
                            modifier = Modifier
                                .width(190.dp)
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
                                .tvFocusableClick(
                                    onClick = { onEvent(PostDetailsState.Event.AnimeSelected(anime.id)) },
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
                                        anime.rating?.let { "★ %.1f".format(it) }).joinToString(" · ")
                                )
                            }
                        }
                    }
                }
            }
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
                    onClick = {
                        if (!voting) onEvent(PostDetailsState.Event.VoteSelected(PostVote.LIKE))
                    },
                )
                PostVoteButton(
                    isLike = false,
                    count = details.reaction.dislikes,
                    selected = details.reaction.vote == PostVote.DISLIKE,
                    onClick = {
                        if (!voting) onEvent(PostDetailsState.Event.VoteSelected(PostVote.DISLIKE))
                    },
                )
                Text(
                    viewsLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedButton(onClick = { onEvent(PostDetailsState.Event.BackSelected) }) {
                    Text(
                        stringResource(R.string.posts_back)
                    )
                }
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
}

@Composable
private fun PostVoteButton(
    isLike: Boolean,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val voteColor = if (isLike) YummySemanticColors.Like else YummySemanticColors.Dislike
    val tint = if (selected) voteColor else voteColor.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .tvFocusableClick(onClick = onClick, shape = shape)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isLike) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = count.toString(),
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun Int.compactCount(): String {
    // stringResource вызывается безусловно (обе ветки), чтобы структура групп
    // композиции была стабильной — иначе падает при prefetch в LazyColumn.
    val millions =
        stringResource(R.string.posts_count_millions, (this / 1_000_000f).compactDecimal())
    val thousands = stringResource(R.string.posts_count_thousands, (this / 1_000f).compactDecimal())
    return when {
        this >= 1_000_000 -> millions
        this >= 1_000 -> thousands
        else -> toString()
    }
}

private fun Float.compactDecimal(): String =
    if (this % 1f == 0f) toInt().toString() else String.format(Locale.US, "%.1f", this)
