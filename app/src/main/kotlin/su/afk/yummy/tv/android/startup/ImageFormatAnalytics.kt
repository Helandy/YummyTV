package su.afk.yummy.tv.android.startup

import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.utils.analyticsParamsOf
import javax.inject.Inject

class ImageFormatAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Декодирует ли устройство AVIF (по пробе): от этого зависит, придёт ли в постерах AVIF или JPEG.
     * Шлётся один раз на процесс при холодном старте.
     *
     * Параметры: avif.
     */
    fun eventImageFormats(avifSupported: Boolean) {
        tracker.track(
            EVENT_IMAGE_FORMATS,
            analyticsParamsOf(
                PARAM_AVIF to avifSupported,
            ),
        )
    }

    private companion object {
        const val EVENT_IMAGE_FORMATS = "image_formats"

        const val PARAM_AVIF = "avif"
    }
}
