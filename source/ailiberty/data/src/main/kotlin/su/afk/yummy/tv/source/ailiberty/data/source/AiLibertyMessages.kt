package su.afk.yummy.tv.source.ailiberty.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.ailiberty.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface AiLibertyMessages {
    val notFound: String
}

internal class AndroidAiLibertyMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : AiLibertyMessages {
    override val notFound: String get() = context.getString(R.string.ailiberty_not_found_message)
}
