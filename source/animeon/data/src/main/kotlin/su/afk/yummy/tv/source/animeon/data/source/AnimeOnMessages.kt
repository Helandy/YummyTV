package su.afk.yummy.tv.source.animeon.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.animeon.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface AnimeOnMessages {
    val notFound: String
    val regionBlocked: String
}

internal class AndroidAnimeOnMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnimeOnMessages {
    override val notFound: String get() = context.getString(R.string.animeon_not_found_message)
    override val regionBlocked: String get() = context.getString(R.string.animeon_region_blocked_message)
}
