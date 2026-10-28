package su.afk.yummy.tv.source.mikai.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.mikai.data.R
import javax.inject.Inject

/** Тексты для пользователя: плеер показывает `message` результата как есть, поэтому они должны быть локализованы. */
internal interface MikaiMessages {
    val notFound: String
    val regionBlocked: String

    /** Пометка к названию команды у субтитров, с ведущим пробелом. */
    val subsSuffix: String
}

internal class AndroidMikaiMessages @Inject constructor(
    @ApplicationContext private val context: Context,
) : MikaiMessages {
    override val notFound: String get() = context.getString(R.string.mikai_not_found_message)
    override val regionBlocked: String get() = context.getString(R.string.mikai_region_blocked_message)
    override val subsSuffix: String get() = context.getString(R.string.mikai_subs_suffix)
}
