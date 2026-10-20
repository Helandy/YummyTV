package su.afk.yummy.tv.core.designsystem.locals

import androidx.compose.runtime.compositionLocalOf

/** Оценки пользователя по `animeId` — чтобы в чужих списках показывать свою рядом с оценкой владельца. */
val LocalLibraryUserRatings = compositionLocalOf<Map<Int, Int>> { emptyMap() }
