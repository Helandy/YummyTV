package su.afk.yummy.tv.android.shortcuts

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.R
import su.afk.yummy.tv.android.InterfaceRouterActivity
import su.afk.yummy.tv.android.continuewatching.loadSoftwarePoster
import su.afk.yummy.tv.android.continuewatching.surfacePosterUrl
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import javax.inject.Inject
import kotlin.math.min

/**
 * Ярлыки по долгому тапу на иконку (мобильный интерфейс): последние тайтлы «Продолжить
 * просмотр» и постоянные «Поиск» / «Загрузки».
 *
 * Все ярлыки динамические: статический `shortcuts.xml` требует литерального `targetPackage`,
 * а у debug/releasedebug-сборок свой суффикс applicationId. Интенты идут в
 * [InterfaceRouterActivity], как и всё внешнее: тайтл — `yummytv://continue/{id}` (сразу плеер),
 * поиск — `ACTION_SEARCH` (его разбирает SystemSearchIntentHandler), загрузки — `yummytv://downloads`.
 */
class AppShortcutsPublisher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
) {

    /** Держит ярлыки в актуальном состоянии, пока жив вызывающий scope. */
    @OptIn(FlowPreview::class)
    suspend fun run() {
        observeContinueWatching()
            .map { items -> items.take(MAX_CONTINUE_SHORTCUTS).map { it.toShortcutTitle() } }
            .distinctUntilChanged()
            .debounce(PUBLISH_DEBOUNCE_MS)
            .collect { titles -> publish(titles) }
    }

    private suspend fun publish(titles: List<ShortcutTitle>) {
        val shortcuts = titles.mapIndexed { index, title -> continueShortcut(title, rank = index) } +
                staticShortcut(
                    id = SEARCH_SHORTCUT_ID,
                    label = context.getString(R.string.shortcut_search),
                    icon = R.drawable.ic_shortcut_search,
                    intent = routerIntent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, ""),
                    rank = titles.size,
                ) +
                staticShortcut(
                    id = DOWNLOADS_SHORTCUT_ID,
                    label = context.getString(R.string.shortcut_downloads),
                    icon = R.drawable.ic_shortcut_downloads,
                    intent = routerIntent(Intent.ACTION_VIEW).setData(DOWNLOADS_DEEP_LINK.toUri()),
                    rank = titles.size + 1,
                )
        // Лимит лаунчера не гарантирован: лишнее с конца (сперва постоянные) просто отбрасываем.
        val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts.take(limit)) }
    }

    private suspend fun continueShortcut(title: ShortcutTitle, rank: Int): ShortcutInfoCompat {
        val poster = title.posterUrl?.let { loadSquareBitmap(it) }
        val icon = poster?.let(IconCompat::createWithAdaptiveBitmap)
            ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        return ShortcutInfoCompat.Builder(context, "$CONTINUE_SHORTCUT_PREFIX${title.animeId}")
            .setShortLabel(title.title)
            .setLongLabel(context.getString(R.string.shortcut_continue_long, title.title))
            .setIcon(icon)
            .setIntent(
                routerIntent(Intent.ACTION_VIEW).setData("$CONTINUE_DEEP_LINK_PREFIX${title.animeId}".toUri()),
            )
            .setRank(rank)
            .build()
    }

    private fun staticShortcut(
        id: String,
        label: String,
        icon: Int,
        intent: Intent,
        rank: Int,
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(label)
        .setIcon(IconCompat.createWithResource(context, icon))
        .setIntent(intent)
        .setRank(rank)
        .build()

    private fun routerIntent(action: String): Intent =
        Intent(context, InterfaceRouterActivity::class.java).setAction(action)

    /** Постер, обрезанный по центру в квадрат: adaptive-иконка сама маскирует края. */
    private suspend fun loadSquareBitmap(url: String): Bitmap? {
        val bitmap = loadSoftwarePoster(context, url, ICON_SIZE_PX) ?: return null
        val side = min(bitmap.width, bitmap.height)
        return Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
            .scale(ICON_SIZE_PX, ICON_SIZE_PX)
    }

    private data class ShortcutTitle(val animeId: Int, val title: String, val posterUrl: String?)

    private companion object {
        fun HomeContinueWatchingItem.toShortcutTitle() = ShortcutTitle(
            animeId = animeId,
            title = animeTitle,
            posterUrl = surfacePosterUrl(),
        )

        /** Лаунчеры показывают ~4 ярлыка: два тайтла + поиск + загрузки. */
        const val MAX_CONTINUE_SHORTCUTS = 2
        const val PUBLISH_DEBOUNCE_MS = 1_000L
        const val ICON_SIZE_PX = 216
        const val CONTINUE_SHORTCUT_PREFIX = "continue_"
        const val SEARCH_SHORTCUT_ID = "search"
        const val DOWNLOADS_SHORTCUT_ID = "downloads"
        const val CONTINUE_DEEP_LINK_PREFIX = "yummytv://continue/"
        const val DOWNLOADS_DEEP_LINK = "yummytv://downloads"
    }
}
