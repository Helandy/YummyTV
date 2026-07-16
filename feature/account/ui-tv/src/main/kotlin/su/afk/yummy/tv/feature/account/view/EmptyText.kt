@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.runtime.Composable
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage

@Composable
internal fun EmptyText(text: String) {
    TvStateMessage(
        title = text,
        fillMaxSize = false,
    )
}
