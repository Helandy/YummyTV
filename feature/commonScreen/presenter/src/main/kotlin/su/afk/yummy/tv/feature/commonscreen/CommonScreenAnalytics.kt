package su.afk.yummy.tv.feature.commonscreen

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import javax.inject.Inject

internal class CommonScreenAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь увидел общий экран ошибки.
     *
     * Параметры: screen и параметры исходного экрана ошибки.
     */
    fun eventErrorShown(params: Map<String, String>) {
        tracker.track(EVENT_ERROR_SHOWN, analyticsParamsOf(PARAM_SCREEN to SCREEN_ERROR) + params)
    }

    /**
     * Пользователь нажал повтор на общем экране ошибки.
     */
    fun eventErrorRetry() {
        tracker.track(EVENT_ERROR_RETRY)
    }

    /**
     * Пользователь вернулся назад с общего экрана ошибки.
     */
    fun eventErrorBack() {
        tracker.track(EVENT_ERROR_BACK)
    }

    /**
     * Пользователь перешел к следующему изображению в общем просмотрщике.
     *
     * Параметры: image_count, selected_index.
     */
    fun eventImageNext(imageCount: Int, selectedIndex: Int) {
        eventImage(EVENT_IMAGE_NEXT, imageCount, selectedIndex)
    }

    /**
     * Пользователь перешел к предыдущему изображению в общем просмотрщике.
     *
     * Параметры: image_count, selected_index.
     */
    fun eventImagePrevious(imageCount: Int, selectedIndex: Int) {
        eventImage(EVENT_IMAGE_PREVIOUS, imageCount, selectedIndex)
    }

    /**
     * Пользователь выбрал изображение по индексу в общем просмотрщике.
     *
     * Параметры: image_count, selected_index.
     */
    fun eventImageSelectIndex(imageCount: Int, selectedIndex: Int) {
        eventImage(EVENT_IMAGE_SELECT_INDEX, imageCount, selectedIndex)
    }

    private fun eventImage(eventName: String, imageCount: Int, selectedIndex: Int) {
        tracker.track(
            eventName,
            analyticsParamsOf(
                PARAM_IMAGE_COUNT to imageCount,
                PARAM_SELECTED_INDEX to selectedIndex,
            ),
        )
    }

    internal companion object {
        private const val PARAM_IMAGE_COUNT = "image_count"
        private const val PARAM_SCREEN = "screen"
        private const val PARAM_SELECTED_INDEX = "selected_index"
        private const val SCREEN_ERROR = "error"

        const val EVENT_ERROR_SHOWN = "error_shown"
        const val EVENT_ERROR_RETRY = "error_retry"
        const val EVENT_ERROR_BACK = "error_back"
        const val EVENT_IMAGE_NEXT = "image_view_next"
        const val EVENT_IMAGE_PREVIOUS = "image_view_previous"
        const val EVENT_IMAGE_SELECT_INDEX = "image_view_select_index"
    }
}
