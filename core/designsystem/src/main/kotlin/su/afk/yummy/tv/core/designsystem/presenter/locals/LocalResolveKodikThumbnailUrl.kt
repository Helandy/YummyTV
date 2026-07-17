package su.afk.yummy.tv.core.designsystem.presenter.locals

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Resolves a Kodik iframe URL to the direct episode thumbnail URL for system APIs
 * that cannot consume the [su.afk.yummy.tv.core.utils.KodikThumbnail] Coil model.
 */
val LocalResolveKodikThumbnailUrl =
    staticCompositionLocalOf<suspend (iframeUrl: String) -> String?> {
        { null }
    }
