package su.afk.yummy.tv.source.sameband.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.sameband.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface SamebandMessages {
    val notFound: String
}

internal class AndroidSamebandMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : SamebandMessages {
    override val notFound: String get() = context.getString(R.string.sameband_not_found_message)
}
