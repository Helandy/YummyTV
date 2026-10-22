package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame
import su.afk.yummy.tv.core.designsystem.components.CollectionAuthorChip
import su.afk.yummy.tv.core.designsystem.focus.tvGridHeaderBringIntoView
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.utils.splitParagraphs
import su.afk.yummy.tv.core.designsystem.utils.toBbAnnotatedString
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.feature.collection.presentation.R

private const val COLLAPSED_DESCRIPTION_LINES = 3

@Composable
internal fun CollectionHeader(
    collection: CollectionDetail,
    isVoteLoading: Boolean,
    isOwner: Boolean,
    isMutationLoading: Boolean,
    onVote: (CollectionVote) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComments: () -> Unit,
    titleFocusRequester: FocusRequester? = null,
    commentsFocusRequester: FocusRequester = remember { FocusRequester() },
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val titleInteractionSource = remember { MutableInteractionSource() }
    val isTitleFocused by titleInteractionSource.collectIsFocusedAsState()
    val likeFocusRequester = remember { FocusRequester() }
    val dislikeFocusRequester = remember { FocusRequester() }
    val editFocusRequester = remember { FocusRequester() }
    val deleteFocusRequester = remember { FocusRequester() }
    var isDescriptionExpanded by remember(collection.description) { mutableStateOf(false) }
    var isDescriptionTruncated by remember(collection.description) { mutableStateOf(false) }
    val detailsFocusRequester = remember { FocusRequester() }
    val paragraphs = remember(collection.description) {
        collection.description.toBbAnnotatedString().splitParagraphs()
    }
    val firstParagraphFocusRequester = remember { FocusRequester() }
    val lastParagraphFocusRequester = remember { FocusRequester() }
    val headerBringIntoViewRequester = remember { BringIntoViewRequester() }
    var wasExpanded by remember { mutableStateOf(false) }
    // «Свернуть» стоит под длинным текстом, и грид остаётся прокручен вглубь: после сворачивания
    // возвращаем шапку целиком (ответчик ниже подменяет запрос на «до упора вверх»).
    LaunchedEffect(isDescriptionExpanded) {
        if (wasExpanded && !isDescriptionExpanded) {
            awaitFrame()
            runCatching { headerBringIntoViewRequester.bringIntoView() }
        }
        wasExpanded = isDescriptionExpanded
    }
    // Кнопка «Показать полностью» стоит под текстом: без переноса фокуса «вниз» после раскрытия
    // перепрыгивал бы сразу на лайки, минуя абзацы. Кадр ждём — иначе абзацы ещё не скомпонованы.
    LaunchedEffect(isDescriptionExpanded) {
        if (isDescriptionExpanded && paragraphs.isNotEmpty()) {
            awaitFrame()
            runCatching { firstParagraphFocusRequester.requestFocus() }
        }
    }
    val hasAuthor = collection.ownerNickname.isNotBlank()
    val hasSideColumn = hasAuthor || isOwner

    Row(
        modifier = modifier
            // Свёрнутая шапка целиком помещается в экран: фокус на лайках/кнопках не должен уводить её
            // вверх. Развёрнутая листается по абзацам обычным bringIntoView.
            .then(if (isDescriptionExpanded) Modifier else Modifier.tvGridHeaderBringIntoView())
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .bringIntoViewRequester(headerBringIntoViewRequester),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (titleFocusRequester != null) {
                            Modifier.focusRequester(titleFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                    .focusProperties {
                        right = if (isOwner) editFocusRequester else FocusRequester.Cancel
                        down = when {
                            isDescriptionExpanded && paragraphs.isNotEmpty() -> firstParagraphFocusRequester
                            isDescriptionTruncated -> detailsFocusRequester
                            else -> likeFocusRequester
                        }
                    }
                    .border(
                        width = 2.dp,
                        color = if (isTitleFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .focusable(interactionSource = titleInteractionSource),
            ) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isTitleFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                )

                if (collection.description.isNotBlank() && !isDescriptionExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = remember(collection.description) { collection.description.toBbAnnotatedString(compact = true) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = COLLAPSED_DESCRIPTION_LINES,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { isDescriptionTruncated = it.hasVisualOverflow },
                    )
                }
            }

            if (isDescriptionExpanded) {
                paragraphs.forEachIndexed { index, paragraph ->
                    CollectionDescriptionParagraph(
                        text = paragraph,
                        modifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstParagraphFocusRequester).focusProperties {
                                        up = titleFocusRequester ?: FocusRequester.Default
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (index == paragraphs.lastIndex) {
                                    Modifier.focusRequester(lastParagraphFocusRequester).focusProperties {
                                        down = detailsFocusRequester
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }

            if (isDescriptionTruncated || isDescriptionExpanded) {
                OutlinedButton(
                    onClick = { isDescriptionExpanded = !isDescriptionExpanded },
                    modifier = Modifier
                        .padding(start = 8.dp, top = 4.dp)
                        .focusRequester(detailsFocusRequester)
                        .focusProperties {
                            if (isDescriptionExpanded && paragraphs.isNotEmpty()) {
                                up = lastParagraphFocusRequester
                            } else if (titleFocusRequester != null) {
                                up = titleFocusRequester
                            }
                            right = if (isOwner) editFocusRequester else FocusRequester.Cancel
                            down = likeFocusRequester
                        },
                ) {
                    Text(
                        stringResource(
                            if (isDescriptionExpanded) {
                                R.string.collection_description_less
                            } else {
                                R.string.collection_description_more
                            },
                        ),
                    )
                }
            }

            CollectionVoteActions(
                collection = collection,
                enabled = !isVoteLoading,
                onVote = onVote,
                onComments = onComments,
                titleFocusRequester = when {
                    isDescriptionTruncated || isDescriptionExpanded -> detailsFocusRequester
                    else -> titleFocusRequester
                },
                likeFocusRequester = likeFocusRequester,
                dislikeFocusRequester = dislikeFocusRequester,
                commentsFocusRequester = commentsFocusRequester,
                downFocusRequester = downFocusRequester,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )
        }

        if (hasSideColumn) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (hasAuthor) {
                    CollectionAuthorChip(
                        nickname = collection.ownerNickname,
                        avatarUrl = collection.ownerAvatarUrl,
                        avatarSize = 36.dp,
                    )
                }
                if (isOwner) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !isMutationLoading,
                        modifier = Modifier
                            .focusRequester(editFocusRequester)
                            .focusProperties {
                                if (titleFocusRequester != null) left = titleFocusRequester
                                down = deleteFocusRequester
                            },
                    ) {
                        Text(stringResource(su.afk.yummy.tv.feature.collection.presentation.R.string.collection_edit))
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        enabled = !isMutationLoading,
                        modifier = Modifier
                            .focusRequester(deleteFocusRequester)
                            .focusProperties {
                                if (titleFocusRequester != null) left = titleFocusRequester
                                up = editFocusRequester
                                if (downFocusRequester != null) down = downFocusRequester
                            },
                    ) {
                        Text(stringResource(su.afk.yummy.tv.core.designsystem.R.string.common_delete))
                    }
                }
            }
        }
    }
}

/** Абзац описания: сам берёт фокус, чтобы длинный текст листался D-pad по блокам, как в новостях. */
@Composable
private fun CollectionDescriptionParagraph(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun CollectionVoteActions(
    collection: CollectionDetail,
    enabled: Boolean,
    onVote: (CollectionVote) -> Unit,
    onComments: () -> Unit,
    titleFocusRequester: FocusRequester?,
    likeFocusRequester: FocusRequester,
    dislikeFocusRequester: FocusRequester,
    commentsFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollectionVoteButton(
            selected = collection.vote == CollectionVote.LIKE,
            count = collection.likesCount,
            vote = CollectionVote.LIKE,
            enabled = enabled,
            onVote = onVote,
            modifier = Modifier
                .focusRequester(likeFocusRequester)
                .collectionVoteFocus(
                    upFocusRequester = titleFocusRequester,
                    rightFocusRequester = dislikeFocusRequester,
                    downFocusRequester = downFocusRequester,
                ),
        )
        CollectionVoteButton(
            selected = collection.vote == CollectionVote.DISLIKE,
            count = collection.dislikesCount,
            vote = CollectionVote.DISLIKE,
            enabled = enabled,
            onVote = onVote,
            modifier = Modifier
                .focusRequester(dislikeFocusRequester)
                .collectionVoteFocus(
                    upFocusRequester = titleFocusRequester,
                    leftFocusRequester = likeFocusRequester,
                    rightFocusRequester = commentsFocusRequester,
                    downFocusRequester = downFocusRequester,
                ),
        )
        OutlinedButton(
            onClick = onComments,
            modifier = Modifier
                .focusRequester(commentsFocusRequester)
                .collectionVoteFocus(
                    upFocusRequester = titleFocusRequester,
                    leftFocusRequester = dislikeFocusRequester,
                    downFocusRequester = downFocusRequester,
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp),
            )
            Text(
                text = stringResource(su.afk.yummy.tv.feature.collection.presentation.R.string.collection_comments)
                    .let { label ->
                        if (collection.commentsCount > 0) "$label · ${collection.commentsCount}" else label
                    },
            )
        }
    }
}

@Composable
private fun CollectionVoteButton(
    selected: Boolean,
    count: Int,
    vote: CollectionVote,
    enabled: Boolean,
    onVote: (CollectionVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLike = vote == CollectionVote.LIKE
    val voteColor = if (isLike) YummySemanticColors.Like else YummySemanticColors.Dislike
    val tint = if (selected) voteColor else voteColor.copy(alpha = 0.72f)
    OutlinedButton(
        modifier = modifier.width(112.dp),
        enabled = enabled,
        onClick = { onVote(vote) },
    ) {
        Icon(
            imageVector = if (isLike) Icons.Filled.ThumbUp else Icons.Filled.ThumbDown,
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

private fun Modifier.collectionVoteFocus(
    upFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester?,
): Modifier =
    focusProperties {
        if (upFocusRequester != null) {
            up = upFocusRequester
        }
        if (leftFocusRequester != null) {
            left = leftFocusRequester
        }
        if (rightFocusRequester != null) {
            right = rightFocusRequester
        }
        if (downFocusRequester != null) {
            down = downFocusRequester
        }
    }
