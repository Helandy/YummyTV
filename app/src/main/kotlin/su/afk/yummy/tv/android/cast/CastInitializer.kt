package su.afk.yummy.tv.android.cast

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.cast.Cast
import androidx.media3.cast.CastParams
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.utils.cast.CastSupport
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Инициализирует media3-cast при старте процесса, если устройство проходит гейт [CastSupport].
 *
 * Manifest-provided Cast options (см. [YummyTvCastOptionsProvider]) по умолчанию заставляют
 * media3-cast использовать устаревший in-app MediaRouteChooserDialog вместо системного Output
 * Switcher - на практике этот диалог не показывает часть реально найденных Cast-устройств,
 * хотя они уже есть в системной таблице MediaRouter2 (проверено логами и системный Cast/VLC
 * те же устройства находят). Явный showSystemOutputSwitcherOnCastButtonClick форсирует
 * системный picker независимо от этого дефолта, receiverApplicationId по-прежнему берётся из
 * манифеста.
 */
@Singleton
class CastInitializer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val castAnalytics: CastAnalytics,
) {
    @OptIn(UnstableApi::class)
    fun initialize() {
        // Сам этот вызов уводит создание CastContext на фоновый поток, где старые GMS роняют
        // процесс - см. CastSupport.
        val decision = CastSupport.decision(context)
        castAnalytics.eventCastUnavailable(decision)
        if (!decision.isSupported) return
        Cast.getSingletonInstance(context).initialize(
            CastParams.Builder()
                .setShowSystemOutputSwitcherOnCastButtonClick(true)
                .build(),
        )
    }
}
