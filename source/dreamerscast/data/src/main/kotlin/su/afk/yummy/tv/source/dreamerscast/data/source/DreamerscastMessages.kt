package su.afk.yummy.tv.source.dreamerscast.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.dreamerscast.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface DreamerscastMessages {
    val notFound: String
}

internal class AndroidDreamerscastMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : DreamerscastMessages {
    override val notFound: String get() = context.getString(R.string.dreamerscast_not_found_message)
}
