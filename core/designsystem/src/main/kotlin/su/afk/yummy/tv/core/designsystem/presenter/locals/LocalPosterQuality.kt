package su.afk.yummy.tv.core.designsystem.presenter.locals

import androidx.compose.runtime.compositionLocalOf
import su.afk.yummy.tv.core.storage.settings.PosterQuality

val LocalPosterQuality = compositionLocalOf { PosterQuality.STANDARD }
