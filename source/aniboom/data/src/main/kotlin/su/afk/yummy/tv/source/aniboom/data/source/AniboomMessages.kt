package su.afk.yummy.tv.source.aniboom.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.aniboom.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface AniboomMessages {
    val notFound: String
}

internal class AndroidAniboomMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : AniboomMessages {
    override val notFound: String get() = context.getString(R.string.aniboom_not_found_message)
}
