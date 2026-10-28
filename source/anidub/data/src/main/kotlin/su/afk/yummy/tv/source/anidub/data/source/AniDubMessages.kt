package su.afk.yummy.tv.source.anidub.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.anidub.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface AniDubMessages {
    val notFound: String
}

internal class AndroidAniDubMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : AniDubMessages {
    override val notFound: String get() = context.getString(R.string.anidub_not_found_message)
}
