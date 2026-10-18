package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

/**
 * Прилипшие вкладки профиля. Когда шапка со стрелкой «назад» уехала, стрелка появляется
 * слева от вкладок — топ-бара на экране нет.
 */
@Composable
internal fun UserProfileStickyTabs(
    tabs: List<UserProfileState.Tab>,
    selected: UserProfileState.Tab,
    profile: UserProfileSummary?,
    showBack: Boolean,
    onBack: () -> Unit,
    onSelected: (UserProfileState.Tab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = showBack,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreR.string.common_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            UserProfileTabs(
                tabs = tabs,
                selected = selected,
                profile = profile,
                onSelected = onSelected,
            )
        }
    }
}
