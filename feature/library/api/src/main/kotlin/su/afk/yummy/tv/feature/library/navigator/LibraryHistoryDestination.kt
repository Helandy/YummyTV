package su.afk.yummy.tv.feature.library.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** История просмотров отдельным корневым экраном (пункт бокового меню ТВ). */
@Serializable
data object LibraryHistoryDestination : NavKey
