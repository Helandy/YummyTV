package su.afk.yummy.tv.feature.commonscreen.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.model.ErrorItem

fun interface IErrorNavigator {
    operator fun invoke(error: ErrorItem): NavKey
}