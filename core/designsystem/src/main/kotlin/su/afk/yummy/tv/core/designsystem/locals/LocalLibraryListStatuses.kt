package su.afk.yummy.tv.core.designsystem.locals

import androidx.compose.runtime.compositionLocalOf
import su.afk.yummy.tv.core.model.anime.CardListStatus

/** Списки пользователя по `animeId` — чтобы карточки тайтлов подписывались без правки их моделей. */
val LocalLibraryListStatuses = compositionLocalOf<Map<Int, CardListStatus>> { emptyMap() }
