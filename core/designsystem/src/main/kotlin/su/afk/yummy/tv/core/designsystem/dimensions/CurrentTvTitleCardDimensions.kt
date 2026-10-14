package su.afk.yummy.tv.core.designsystem.dimensions

import androidx.compose.runtime.Composable

/** Размер карточки в гридах совпадает с карточкой на главной, чтобы постеры были одной высоты. */
@Composable
fun currentTvTitleCardDimensions(): PosterCardDimensions = currentTvHomeFeedCardDimensions()
