package su.afk.yummy.tv.source.anime4k.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.anime4k.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface Anime4kMessages {
    val declined: String
    val adNotShown: String
    val rateLimited: String
    val notFound: String
}

internal class AndroidAnime4kMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : Anime4kMessages {
    override val declined: String get() = context.getString(R.string.anime4k_declined_message)
    override val adNotShown: String get() = context.getString(R.string.anime4k_ad_not_shown_message)
    override val rateLimited: String get() = context.getString(R.string.anime4k_rate_limited_message)
    override val notFound: String get() = context.getString(R.string.anime4k_not_found_message)
}
