package su.afk.yummy.tv.domain.watching.model

import su.afk.yummy.tv.core.model.watching.ContinueWatchingLaunch
import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration

internal data class ContinueWatchingLaunchResolution(
    val launch: ContinueWatchingLaunch,
    val progressMigration: ContinueWatchingProgressMigration? = null,
)
