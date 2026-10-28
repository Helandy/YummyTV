package su.afk.yummy.tv.source.animebesst.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.animebesst.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface AnimebesstMessages {
    val notFound: String
}

internal class AndroidAnimebesstMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnimebesstMessages {
    override val notFound: String get() = context.getString(R.string.animebesst_not_found_message)
}
