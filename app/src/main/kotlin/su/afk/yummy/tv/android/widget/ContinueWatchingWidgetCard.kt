package su.afk.yummy.tv.android.widget

import android.graphics.Bitmap

/** Готовая к отрисовке карточка виджета: постер уже загружен, тексты собраны из ресурсов. */
internal data class ContinueWatchingWidgetCard(
    val animeId: Int,
    val title: String,
    val subtitle: String,
    val poster: Bitmap?,
)
