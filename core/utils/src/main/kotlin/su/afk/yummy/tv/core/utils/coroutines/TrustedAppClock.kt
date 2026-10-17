package su.afk.yummy.tv.core.utils.coroutines

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.common.coroutines.AppClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Часы приложения на доверенном времени Google (TrustedTime): системные часы приставок без NTP
 * уезжают на дни и ломают «Через N часов», «сегодня» в расписании и подобное.
 *
 * Пока клиент не создан, либо на устройстве нет Google Play services (кастомные прошивки), отдаём
 * системное время - то есть поведение там прежнее. Клиент поднимается один раз через [start].
 */
@Singleton
class TrustedAppClock @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppClock {

    @Volatile
    private var client: TrustedTimeClient? = null

    /** Поднимает TrustedTime в фоне; любые сбои GMS тихо оставляют системное время. */
    fun start() {
        runCatching {
            val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            if (available != ConnectionResult.SUCCESS) return
            TrustedTime.createClient(context).addOnSuccessListener { client = it }
        }
    }

    override fun nowMillis(): Long =
        runCatching { client?.computeCurrentUnixEpochMillis() }.getOrNull() ?: System.currentTimeMillis()
}
