package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun LinkedAccountProvider.label(): String = stringResource(
    when (this) {
        LinkedAccountProvider.VK -> R.string.profile_linked_vk
        LinkedAccountProvider.TELEGRAM -> R.string.profile_linked_telegram
        LinkedAccountProvider.DISCORD -> R.string.profile_linked_discord
        LinkedAccountProvider.SHIKIMORI -> R.string.profile_linked_shikimori
    }
)
