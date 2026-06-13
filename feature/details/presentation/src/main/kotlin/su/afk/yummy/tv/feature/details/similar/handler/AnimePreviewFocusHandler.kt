package su.afk.yummy.tv.feature.details.similar.handler

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
        isCurrentFocus: () -> Boolean,
        onCachedPreview: (AnimePreview?, Map<Int, AnimePreview>) -> Unit,
        onLoadedPreview: (AnimePreviewFocusResult) -> Unit,
    ) {
        loader.focus(
            scope = scope,
            key = animeId,
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
}

internal data class AnimePreviewFocusResult(
    val animeId: Int,
    val preview: AnimePreview,
    val previews: Map<Int, AnimePreview>,
    val isCurrentFocus: Boolean,
)
