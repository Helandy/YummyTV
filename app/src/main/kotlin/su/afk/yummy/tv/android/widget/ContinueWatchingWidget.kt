package su.afk.yummy.tv.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.R
import su.afk.yummy.tv.android.InterfaceRouterActivity
import su.afk.yummy.tv.android.continuewatching.loadSoftwarePoster
import su.afk.yummy.tv.android.continuewatching.surfacePosterUrl
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase

/**
 * Виджет «Продолжить просмотр» (мобильный рабочий стол): последние тайтлы из того же списка,
 * что на главной. Тап по тайтлу — `yummytv://continue/{id}`, то есть сразу плеер.
 *
 * Пока сессия виджета жива, список приходит из потока; новую сессию запускает
 * [ContinueWatchingWidgetUpdater], пока приложение открыто: список локальный и вне его не меняется.
 */
class ContinueWatchingWidget : GlanceAppWidget() {

    // Exact: число карточек зависит от фактической ширины, которую отдаёт лаунчер.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val observeContinueWatching = entryPoint.observeContinueWatchingUseCase()
        val initialCards = runSuspendCatching {
            loadCards(context, observeContinueWatching().first())
        }.getOrDefault(emptyList())
        provideContent {
            // Список читается внутри композиции: updateAll при живой сессии только
            // перекомпоновывает её, не вызывая provideGlance заново.
            var cards by remember { mutableStateOf(initialCards) }
            LaunchedEffect(Unit) {
                observeContinueWatching()
                    .distinctUntilChanged()
                    .collectLatest { items ->
                        runSuspendCatching { loadCards(context, items) }.onSuccess { cards = it }
                    }
            }
            GlanceTheme {
                WidgetContent(cards)
            }
        }
    }

    private suspend fun loadCards(
        context: Context,
        items: List<HomeContinueWatchingItem>,
    ): List<ContinueWatchingWidgetCard> = items.take(MAX_CARDS).map { item ->
        ContinueWatchingWidgetCard(
            animeId = item.animeId,
            title = item.animeTitle,
            subtitle = item.widgetSubtitle(context),
            poster = item.surfacePosterUrl()?.let { loadSoftwarePoster(context, it, POSTER_WIDTH_PX) },
        )
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun observeContinueWatchingUseCase(): ObserveContinueWatchingUseCase
    }

    companion object {
        const val MAX_CARDS = 3

        /**
         * Ширина постера: карточка узкого виджета ~170dp. Крупнее не берём — bitmap'ы всех
         * размеров виджета держит лаунчер, и их суммарный объём ограничен системой.
         */
        private const val POSTER_WIDTH_PX = 400
    }
}

@Composable
private fun WidgetContent(cards: List<ContinueWatchingWidgetCard>) {
    val context = LocalContext.current
    // Карточка не уже ~110dp: в узкий виджет — одна, в широкий — до трёх.
    val maxItems = (LocalSize.current.width / MIN_CARD_WIDTH).toInt()
        .coerceIn(1, ContinueWatchingWidget.MAX_CARDS)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        Text(
            text = context.getString(R.string.widget_continue_watching_title),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(8.dp))
        if (cards.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(routerIntent(context, null))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = context.getString(R.string.widget_continue_watching_empty),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
        } else {
            Row(modifier = GlanceModifier.fillMaxSize()) {
                cards.take(maxItems).forEachIndexed { index, card ->
                    if (index > 0) Spacer(GlanceModifier.width(8.dp))
                    WidgetCard(card = card, modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(card: ContinueWatchingWidgetCard, modifier: GlanceModifier) {
    val context = LocalContext.current
    // Ширину задаёт вес в ряду: fillMaxSize здесь перебил бы его и отдал всё первой карточке.
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(actionStartActivity(routerIntent(context, card.animeId))),
    ) {
        val posterModifier = GlanceModifier
            .fillMaxWidth()
            .defaultWeight()
            .cornerRadius(8.dp)
        if (card.poster != null) {
            Image(
                provider = ImageProvider(card.poster),
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = posterModifier,
            )
        } else {
            Box(modifier = posterModifier.background(GlanceTheme.colors.surfaceVariant)) {}
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = card.title,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
            maxLines = 1,
        )
        Text(
            text = card.subtitle,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            maxLines = 1,
        )
    }
}

private val MIN_CARD_WIDTH = 110.dp

/** Тайтл — сразу плеер через `yummytv://continue/{id}`; без тайтла — просто открыть приложение. */
private fun routerIntent(context: Context, animeId: Int?): Intent =
    Intent(context, InterfaceRouterActivity::class.java).apply {
        if (animeId != null) {
            action = Intent.ACTION_VIEW
            data = "yummytv://continue/$animeId".toUri()
        } else {
            action = Intent.ACTION_MAIN
        }
    }
