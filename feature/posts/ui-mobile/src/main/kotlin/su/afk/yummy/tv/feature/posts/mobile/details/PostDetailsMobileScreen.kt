package su.afk.yummy.tv.feature.posts.mobile.details

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileFullscreenImageDialog
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.domain.posts.model.PostDetails
import su.afk.yummy.tv.feature.posts.details.PostDetailsState
import su.afk.yummy.tv.feature.posts.mobile.R
import su.afk.yummy.tv.feature.posts.mobile.view.PostDetailsHeader
import su.afk.yummy.tv.feature.posts.mobile.view.PostEngagementPanel
import su.afk.yummy.tv.feature.posts.model.PostContentBlock
import su.afk.yummy.tv.feature.posts.utils.parsePostContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsMobileScreen(
    state: PostDetailsState.State,
    effect: Flow<PostDetailsState.Effect>,
    onEvent: (PostDetailsState.Event) -> Unit,
) {
    var fullscreenImageUrl by rememberSaveable { mutableStateOf<String?>(null) }

    fullscreenImageUrl?.let { imageUrl ->
        MobileFullscreenImageDialog(
            model = imageUrl,
            contentDescription = state.details?.title,
            closeContentDescription = stringResource(R.string.posts_close_image),
            onDismiss = { fullscreenImageUrl = null },
        )
    }

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
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.posts_publication),
                onBack = { onEvent(PostDetailsState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.loading,
            error = state.error?.takeIf { it.isNotBlank() }
                ?: if (state.error != null) stringResource(R.string.posts_error) else null,
            onRetry = { onEvent(PostDetailsState.Event.RetrySelected) },
            empty = !state.loading && state.error == null && state.details == null,
            emptyText = stringResource(R.string.posts_empty),
        ) {
            PostDetailsBody(
                details = requireNotNull(state.details),
                voting = state.voting,
                onEvent = onEvent,
                onImageClick = { fullscreenImageUrl = it },
            )
        }
    }
}

@Composable
private fun PostDetailsBody(
    details: PostDetails,
    voting: Boolean,
    onEvent: (PostDetailsState.Event) -> Unit,
    onImageClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PostDetailsHeader(
                details = details,
                onAuthorClick = {
                    onEvent(PostDetailsState.Event.AuthorSelected(details.author.id))
                },
            )
        }
        details.previewImageUrl?.takeIf(String::isNotBlank)?.let { url ->
            item {
                AsyncImage(
                    url,
                    details.title,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onImageClick(url) },
                    contentScale = ContentScale.Crop
                )
            }
        }
        items(details.contentHtml.parsePostContent(details.previewImageUrl)) { block ->
            when (block) {
                is PostContentBlock.Text -> Text(
                    block.value,
                    style = MaterialTheme.typography.bodyLarge
                )

                is PostContentBlock.Image -> AsyncImage(
                    block.url,
                    block.description,
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onImageClick(block.url) },
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
        if (details.relatedAnime.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.posts_related_anime),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(details.relatedAnime, key = { it.id }) { anime ->
                        ElevatedCard(
                            onClick = { onEvent(PostDetailsState.Event.AnimeSelected(anime.id)) },
                            modifier = Modifier.width(150.dp)
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
                                Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    anime.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2
                                )
                                Text(
                                    listOfNotNull(
                                        anime.year?.toString(),
                                        anime.rating?.let { "★ %.1f".format(it) }).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            PostEngagementPanel(
                reaction = details.reaction,
                comments = details.comments,
                voting = voting,
                onVote = { onEvent(PostDetailsState.Event.VoteSelected(it)) },
                onCommentsClick = { onEvent(PostDetailsState.Event.CommentsSelected) },
            )
        }
    }
}
