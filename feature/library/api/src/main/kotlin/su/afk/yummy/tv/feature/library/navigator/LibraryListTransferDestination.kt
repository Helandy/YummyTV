package su.afk.yummy.tv.feature.library.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Экран импорта и экспорта списка аниме в форматах Shikimori и MyAnimeList. */
@Serializable
data object LibraryListTransferDestination : NavKey
