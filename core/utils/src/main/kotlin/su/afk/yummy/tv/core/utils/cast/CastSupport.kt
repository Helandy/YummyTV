package su.afk.yummy.tv.core.utils.cast

import android.content.Context
import android.content.res.Configuration
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import su.afk.yummy.tv.core.utils.cast.CastSupport.MIN_GMS_APK_VERSION
import su.afk.yummy.tv.core.utils.cast.CastSupport.decision

/**
 * Единственный гейт на весь Cast в приложении.
 *
 * media3 1.11.1 создаёт CastContext на фоновом потоке
 * (`Cast.initialize` -> `CastContext.getSharedInstance(context, BackgroundExecutor.get(), ...)`).
 * Dynamite-модули GMS старше августа 2022 этого не умеют: конструктор CastContextImpl дёргает
 * addListener, который требует главного потока, и кидает IllegalStateException прямо в чужом
 * потоке. Перехватить его нельзя - GMS заворачивает в Task только ModuleUnavailableException,
 * всё остальное улетает как uncaught и роняет процесс (краш на приставках с GMS 13.2.80).
 *
 * Поэтому на таких устройствах Cast не трогаем вообще: гейт стоит перед всеми тремя входами в
 * этот фоновый путь - инициализацией в Application, RemoteCastPlayer в плеер-сервисе и
 * MediaRouteButton в мобильной шторке.
 *
 * Сам по себе класс ничего не логирует и не репортит: решение отдаётся наружу через [decision],
 * а в аналитику его отправляет :app (CastAnalytics) - один раз на процесс, на старте.
 */
object CastSupport {

    /**
     * GMS APK августа 2022 (22.26.x) - времён Cast SDK 21.1.0, где появился асинхронный
     * `CastContext.getSharedInstance(Context, Executor)`. Дефолтной перегрузки
     * [GoogleApiAvailability.isGooglePlayServicesAvailable] тут мало: она сверяется с константой
     * google_play_services_version из play-services-basement (12451000), которая ниже версии
     * падающих устройств, и возвращает SUCCESS.
     */
    private const val MIN_GMS_APK_VERSION = 222_600_000

    /** Почему Cast включён или выключен. */
    enum class Status {
        SUPPORTED,

        /** Кастовать с приставки некуда. */
        TELEVISION,

        /** GMS старше [MIN_GMS_APK_VERSION] либо его нет вовсе. */
        PLAY_SERVICES_TOO_OLD,

        /** Сама проверка не отработала - Cast выключаем на всякий случай. */
        CHECK_FAILED,
    }

    /**
     * @param gmsApkVersion версия установленного GMS (та же, что в шапке крашей AppMetrica),
     * null - если получить не удалось.
     * @param availabilityCode код [ConnectionResult] от проверки Play Services.
     */
    data class Decision(
        val status: Status,
        val gmsApkVersion: Int? = null,
        val availabilityCode: Int? = null,
        val error: Throwable? = null,
    ) {
        val isSupported: Boolean get() = status == Status.SUPPORTED
    }

    @Volatile
    private var cached: Decision? = null

    /**
     * Снимок решения без побочных эффектов - для UI-гейтов, где считать проверку на каждую
     * рекомпозицию незачем. До первого [decision] с контекстом возвращает false.
     */
    val isSupported: Boolean
        get() = cached?.isSupported == true

    /** Можно ли трогать Cast в этом процессе. */
    fun isSupported(context: Context): Boolean = decision(context).isSupported

    /** Решение с причиной. Считается один раз и кешируется на процесс. */
    fun decision(context: Context): Decision =
        cached ?: evaluate(context).also { cached = it }

    private fun evaluate(context: Context): Decision {
        // На ТВ кастовать некуда - и ровно там чаще всего живут древние GMS кастомных прошивок
        // (тот же рантайм-детект, что в DeviceAwareTvIntegration: :app - один APK на обе платформы).
        if (isTelevision(context)) return Decision(Status.TELEVISION)
        val availability = GoogleApiAvailability.getInstance()
        return try {
            val code = availability.isGooglePlayServicesAvailable(context, MIN_GMS_APK_VERSION)
            Decision(
                status = if (code == ConnectionResult.SUCCESS) {
                    Status.SUPPORTED
                } else {
                    Status.PLAY_SERVICES_TOO_OLD
                },
                gmsApkVersion = availability.runCatching { getApkVersion(context) }.getOrNull(),
                availabilityCode = code,
            )
        } catch (e: Throwable) {
            Decision(status = Status.CHECK_FAILED, error = e)
        }
    }

    private fun isTelevision(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
}
