package su.afk.yummy.tv.feature.settings.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.model.DetailsButtonOrderItem

@Composable
internal fun List<DetailsButtonAction>.toDetailsButtonOrderItems(): List<DetailsButtonOrderItem> =
    buildList {
        var index = 0
        while (index <= this@toDetailsButtonOrderItems.lastIndex) {
            val action = this@toDetailsButtonOrderItems[index]
            val nextAction = this@toDetailsButtonOrderItems.getOrNull(index + 1)
            if (action == DetailsButtonAction.LIBRARY && nextAction == DetailsButtonAction.FAVORITE) {
                add(
                    DetailsButtonOrderItem(
                        key = "LIBRARY_FAVORITE",
                        action = DetailsButtonAction.LIBRARY,
                        label = stringResource(R.string.settings_details_button_library_favorite),
                    ),
                )
                index += 2
            } else if (action != DetailsButtonAction.FAVORITE) {
                add(
                    DetailsButtonOrderItem(
                        key = action.name,
                        action = action,
                        label = action.label(),
                    ),
                )
                index += 1
            } else {
                index += 1
            }
        }
    }

@Composable
internal fun AppTheme.label(): String = stringResource(
    when (this) {
        AppTheme.WARM_AMBER -> R.string.settings_theme_warm_amber
        AppTheme.SAKURA -> R.string.settings_theme_sakura
        AppTheme.MINT -> R.string.settings_theme_mint
        AppTheme.OCEAN -> R.string.settings_theme_ocean
        AppTheme.GRAPHITE -> R.string.settings_theme_graphite
    },
)

@Composable
internal fun AppTheme.hint(): String = stringResource(
    when (this) {
        AppTheme.WARM_AMBER -> R.string.settings_theme_warm_amber_hint
        AppTheme.SAKURA -> R.string.settings_theme_sakura_hint
        AppTheme.MINT -> R.string.settings_theme_mint_hint
        AppTheme.OCEAN -> R.string.settings_theme_ocean_hint
        AppTheme.GRAPHITE -> R.string.settings_theme_graphite_hint
    },
)

@Composable
internal fun DetailsButtonAction.label(): String = stringResource(
    when (this) {
        DetailsButtonAction.WATCH -> R.string.settings_details_button_watch
        DetailsButtonAction.LIBRARY -> R.string.settings_details_button_library
        DetailsButtonAction.FAVORITE -> R.string.settings_details_button_favorite
        DetailsButtonAction.EPISODES -> R.string.settings_details_button_episodes
        DetailsButtonAction.SUBSCRIPTIONS -> R.string.settings_details_button_subscriptions
        DetailsButtonAction.FULL_DETAILS -> R.string.settings_details_button_full_details
        DetailsButtonAction.TRAILERS -> R.string.settings_details_button_trailers
        DetailsButtonAction.SIMILAR -> R.string.settings_details_button_similar
        DetailsButtonAction.VIEWING_ORDER -> R.string.settings_details_button_viewing_order
        DetailsButtonAction.RATING -> R.string.settings_details_button_rating
        DetailsButtonAction.COLLECTIONS -> R.string.settings_details_button_collections
        DetailsButtonAction.SCREENSHOTS -> R.string.settings_details_button_screenshots
    },
)

@Composable
internal fun PosterQuality.label(): String = stringResource(
    when (this) {
        PosterQuality.LOW -> R.string.settings_poster_quality_low
        PosterQuality.STANDARD -> R.string.settings_poster_quality_standard
        PosterQuality.MEGA -> R.string.settings_poster_quality_mega
        PosterQuality.HIGH -> R.string.settings_poster_quality_high
    },
)

@Composable
internal fun PosterQuality.hint(): String = stringResource(
    when (this) {
        PosterQuality.LOW -> R.string.settings_poster_quality_low_hint
        PosterQuality.STANDARD -> R.string.settings_poster_quality_standard_hint
        PosterQuality.MEGA -> R.string.settings_poster_quality_mega_hint
        PosterQuality.HIGH -> R.string.settings_poster_quality_high_hint
    },
)

@Composable
internal fun PreferredPlayer.label(): String = when (this) {
    PreferredPlayer.NONE -> stringResource(R.string.settings_preferred_player_none)
    PreferredPlayer.KODIK -> stringResource(R.string.settings_preferred_player_kodik)
    PreferredPlayer.AKSOR -> stringResource(R.string.settings_preferred_player_aksor)
    PreferredPlayer.ALLOHA -> stringResource(R.string.settings_preferred_player_alloha)
    PreferredPlayer.CVH -> stringResource(R.string.settings_preferred_player_cvh)
    PreferredPlayer.VK -> stringResource(R.string.settings_preferred_player_vk)
}

@Composable
internal fun PreferredPlayer.hint(): String = when (this) {
    PreferredPlayer.NONE -> stringResource(R.string.settings_preferred_player_none_hint)
    PreferredPlayer.KODIK -> stringResource(R.string.settings_preferred_player_kodik_hint)
    PreferredPlayer.AKSOR -> stringResource(R.string.settings_preferred_player_aksor_hint)
    PreferredPlayer.ALLOHA -> stringResource(R.string.settings_preferred_player_alloha_hint)
    PreferredPlayer.CVH -> stringResource(R.string.settings_preferred_player_cvh_hint)
    PreferredPlayer.VK -> stringResource(R.string.settings_preferred_player_vk_hint)
}

@Composable
internal fun PreviewCacheSize.label(): String = stringResource(
    when (this) {
        PreviewCacheSize.MB_50 -> R.string.settings_cache_size_min
        PreviewCacheSize.MB_100 -> R.string.settings_cache_size_standard
        PreviewCacheSize.MB_200 -> R.string.settings_cache_size_large
        PreviewCacheSize.MB_300 -> R.string.settings_cache_size_max
    },
)

@Composable
internal fun PreviewCacheSize.hint(): String = stringResource(
    when (this) {
        PreviewCacheSize.MB_50 -> R.string.settings_cache_size_50
        PreviewCacheSize.MB_100 -> R.string.settings_cache_size_100
        PreviewCacheSize.MB_200 -> R.string.settings_cache_size_200
        PreviewCacheSize.MB_300 -> R.string.settings_cache_size_300
    },
)
