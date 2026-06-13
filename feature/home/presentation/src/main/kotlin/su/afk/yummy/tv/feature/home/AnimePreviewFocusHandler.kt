package su.afk.yummy.tv.feature.home

import kotlinx.coroutines.CoroutineScope
import su.afk.yummy.tv.core.utils.DebouncedCachedLoader
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.anime.usecase.GetAnimePreviewUseCase
import javax.inject.Inject

internal class AnimePreviewFocusHandler @Inject constructor(
    getAnimePreview: GetAnimePreviewUseCase,
) {
    private val loader = DebouncedCachedLoader<Int, AnimePreview>(
        loadValue = { animeId -> getAnimePreview(animeId) },
    )

    fun focus(
        scope: CoroutineScope,
        animeId: Int,
        debounceMs: Long,
        isCurrentFocus: () -> Boolean,
        onCachedPreview: (AnimePreview?, Map<Int, AnimePreview>) -> Unit,
        onLoadedPreview: (AnimePreviewFocusResult) -> Unit,
    ) {
        loader.focus(
            scope = scope,
            key = animeId,
            debounceMs = debounceMs,
            isCurrentFocus = isCurrentFocus,
            onCachedValue = onCachedPreview,
            onLoadedValue = { result ->
                onLoadedPreview(
                    AnimePreviewFocusResult(
                        animeId = result.key,
                        preview = result.value,
                        previews = result.cache,
                        isCurrentFocus = result.isCurrentFocus,
                    )
                )
            },
        )
    }

    fun cancelFocus() {
        loader.cancelFocus()
    }

    fun prefetch(
        scope: CoroutineScope,
        animeId: Int,
        onCacheChanged: (Map<Int, AnimePreview>) -> Unit,
    ) {
        loader.prefetch(scope, animeId, onCacheChanged)
    }
}

internal data class AnimePreviewFocusResult(
    val animeId: Int,
    val preview: AnimePreview,
    val previews: Map<Int, AnimePreview>,
    val isCurrentFocus: Boolean,
)
