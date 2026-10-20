package su.afk.yummy.tv.source.anistar.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.anistar.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface AnistarMessages {
    val notFound: String
}

internal class AndroidAnistarMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnistarMessages {
    override val notFound: String get() = context.getString(R.string.anistar_not_found_message)
}
