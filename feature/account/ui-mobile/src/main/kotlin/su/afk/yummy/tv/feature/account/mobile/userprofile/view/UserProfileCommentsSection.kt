package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileSectionLabel
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileSurfacePanel
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.feature.account.userprofile.model.UserProfileCommentPreview

/** Свежие комментарии пользователя под тайтлами; тап открывает тайтл, полный список — по кнопке. */
@Composable
internal fun UserProfileCommentsSection(
    comments: UserProfileState.PagedContent<UserProfileCommentPreview>,
    onEvent: (UserProfileState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowMillis = remember(comments.items) { System.currentTimeMillis() }
    AccountMobileSurfacePanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountMobileSectionLabel(
                icon = Icons.Filled.ChatBubbleOutline,
                title = stringResource(R.string.account_profile_social_comments),
            )
            when {
                comments.isLoading && comments.items.isEmpty() -> AccountMobileLoadingIndicator()

                comments.error && comments.items.isEmpty() -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.user_profile_comments_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onEvent(UserProfileState.Event.RetryCommentsSelected) }) {
                        Text(stringResource(CoreR.string.common_retry))
                    }
                }

                comments.items.isEmpty() -> Text(
                    text = stringResource(R.string.user_profile_comments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> comments.items.forEach { comment ->
                    UserProfileCommentRow(
                        comment = comment,
                        nowMillis = nowMillis,
                        onClick = {
                            onEvent(
                                comment.animeId?.let { UserProfileState.Event.CommentAnimeSelected(it) }
                                    ?: UserProfileState.Event.CommentsSelected,
                            )
                        },
                    )
                }
            }
            FilledTonalButton(
                onClick = { onEvent(UserProfileState.Event.CommentsSelected) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.user_profile_comments_all))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
