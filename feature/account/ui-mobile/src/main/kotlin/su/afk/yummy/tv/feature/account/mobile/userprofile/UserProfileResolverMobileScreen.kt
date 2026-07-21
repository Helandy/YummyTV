package su.afk.yummy.tv.feature.account.mobile.userprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileResolverState
import su.afk.yummy.tv.core.designsystem.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileResolverMobileScreen(
    state: UserProfileResolverState.State,
    effect: Flow<UserProfileResolverState.Effect>,
    onEvent: (UserProfileResolverState.Event) -> Unit,
) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.user_profile_title),
                onBack = { onEvent(UserProfileResolverState.Event.BackSelected) },
            )
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.isLoading) CircularProgressIndicator()
            else MobileMessage(
                title = stringResource(R.string.user_profile_not_found),
                icon = Icons.Default.PersonOff,
                actionLabel = stringResource(CoreR.string.retry),
                onAction = { onEvent(UserProfileResolverState.Event.RetrySelected) },
            )
        }
    }
}
