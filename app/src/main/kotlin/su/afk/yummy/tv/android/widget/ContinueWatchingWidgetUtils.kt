package su.afk.yummy.tv.android.widget

import android.content.Context
import su.afk.yummy.tv.R
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem

/** «Серия 3 · 12:30 / 24:00» — как подпись карточки на главной. */
internal fun HomeContinueWatchingItem.widgetSubtitle(context: Context): String {
    val episodeText = if (episode.isBlank()) {
        context.getString(R.string.widget_episode)
    } else {
        context.getString(R.string.widget_episode_number, episode)
    }
    val timing = if (durationMs > 0L) {
        "${positionMs.millisToClockTime()} / ${durationMs.millisToClockTime()}"
    } else {
        positionMs.millisToClockTime()
    }
    return "$episodeText · $timing"
}
