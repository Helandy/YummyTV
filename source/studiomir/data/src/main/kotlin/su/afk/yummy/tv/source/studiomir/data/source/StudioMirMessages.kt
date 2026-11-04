package su.afk.yummy.tv.source.studiomir.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.studiomir.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface StudioMirMessages {
    val notFound: String
}

internal class AndroidStudioMirMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : StudioMirMessages {
    override val notFound: String get() = context.getString(R.string.studiomir_not_found_message)
}
