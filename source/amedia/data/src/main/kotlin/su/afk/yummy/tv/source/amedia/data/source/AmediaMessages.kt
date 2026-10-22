package su.afk.yummy.tv.source.amedia.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.amedia.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface AmediaMessages {
    val notFound: String
}

internal class AndroidAmediaMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : AmediaMessages {
    override val notFound: String get() = context.getString(R.string.amedia_not_found_message)
}
