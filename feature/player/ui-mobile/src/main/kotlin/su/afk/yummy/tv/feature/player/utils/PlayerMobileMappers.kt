package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips

internal fun PlayerSkips.segments(): List<Pair<String, PlayerSkipSegment>> =
    buildList {
        opening?.let { add("opening" to it) }
        ending?.let { add("ending" to it) }
    }
