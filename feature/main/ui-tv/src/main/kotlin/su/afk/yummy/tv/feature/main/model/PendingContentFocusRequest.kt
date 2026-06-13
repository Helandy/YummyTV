package su.afk.yummy.tv.feature.main.model

import su.afk.yummy.tv.core.navigation.root.RootTab

internal data class PendingContentFocusRequest(
    val root: RootTab,
    val token: Int,
)
