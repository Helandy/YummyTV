package su.afk.yummy.tv.feature.posts.mobile.details

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileFullscreenImageDialog
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.posts.details.PostDetailsState
import su.afk.yummy.tv.feature.posts.mobile.R
import su.afk.yummy.tv.feature.posts.mobile.view.PostDetailsBody

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
