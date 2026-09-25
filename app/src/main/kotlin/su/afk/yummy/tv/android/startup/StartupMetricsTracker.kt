package su.afk.yummy.tv.android.startup

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.android.InterfaceRouterActivity
import su.afk.yummy.tv.android.MobileActivity
import su.afk.yummy.tv.android.TvActivity
import su.afk.yummy.tv.android.startup.model.StartupEntry
import su.afk.yummy.tv.android.startup.model.StartupMetrics
import su.afk.yummy.tv.android.startup.model.StartupUi
import su.afk.yummy.tv.android.startup.utils.isProcessStartedInForeground
import su.afk.yummy.tv.android.startup.utils.lastProcessStartInfo
import su.afk.yummy.tv.android.startup.utils.startReasonName
import su.afk.yummy.tv.android.startup.utils.startTypeName
import su.afk.yummy.tv.android.startup.utils.toStartupEntry
import su.afk.yummy.tv.android.startup.utils.totalRamGb
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Полевой замер холодного старта: от старта процесса до первого кадра главной Activity (TTID)
 * и до reportFullyDrawn с главной (TTFD). Лабораторный аналог — StartupBenchmark в :baselineprofile.
 *
 * Замер отбрасывается, если процесс поднят в фоне (WorkManager, пуш, поиск), если это первый
 * запуск с диалогом выбора интерфейса, если Activity восстанавливается из savedInstanceState или
 * создаётся сильно позже Application — всё это не тот старт, который видит пользователь.
 */
@Singleton
class StartupMetricsTracker @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val analytics: StartupAnalytics,
    private val interfaceModePreferences: AppInterfaceModePreferences,
) : Application.ActivityLifecycleCallbacks {

    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finish() }
    private val processStartedAt = Process.getStartUptimeMillis()

    private var application: Application? = null
    private var onCreateStartedAt = 0L
    private var onCreateFinishedAt = 0L
    private var entry: StartupEntry? = null
    private var ui: StartupUi? = null
    private var isFirstFrameListenerInstalled = false
    private var firstFrameAt: Long? = null
    private var fullyDrawnAt: Long? = null
    private var isFinished = false

    /** Вызывать сразу после super.onCreate() приложения; [onCreateStartedAt] — до него. */
    fun start(application: Application, onCreateStartedAt: Long) {
        this.onCreateStartedAt = onCreateStartedAt
        if (!isProcessStartedInForeground() || interfaceModePreferences.selectedMode == null) {
            isFinished = true
            return
        }
        this.application = application
        application.registerActivityLifecycleCallbacks(this)
        handler.postDelayed(timeout, TIMEOUT_MS)
    }

    /** Конец Application.onCreate. */
    fun onApplicationCreated() {
        onCreateFinishedAt = SystemClock.uptimeMillis()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (isFinished) return
        if (entry == null) entry = activity.intent.toStartupEntry()
        when (activity) {
            is InterfaceRouterActivity -> Unit
            is MobileActivity -> onMainActivityCreated(activity, StartupUi.MOBILE, savedInstanceState)
            is TvActivity -> onMainActivityCreated(activity, StartupUi.TV, savedInstanceState)
            else -> release()
        }
    }

    private fun onMainActivityCreated(
        activity: ComponentActivity,
        ui: StartupUi,
        savedInstanceState: Bundle?,
    ) {
        if (this.ui != null) return
        val sinceApplicationCreated = SystemClock.uptimeMillis() - onCreateFinishedAt
        if (savedInstanceState != null || sinceApplicationCreated > MAX_ACTIVITY_DELAY_MS) {
            release()
            return
        }
        this.ui = ui
        activity.fullyDrawnReporter.addOnReportDrawnListener { onFullyDrawn() }
    }

    override fun onActivityResumed(activity: Activity) {
        if (isFinished || isFirstFrameListenerInstalled || !activity.isMainActivity()) return
        isFirstFrameListenerInstalled = true
        // ViewRootImpl добавляется после onResume, так что первый кадр ещё впереди
        val decorView = activity.window.decorView
        decorView.viewTreeObserver.addOnDrawListener(FirstDrawListener(decorView))
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.isMainActivity()) finish()
    }

    private fun onFirstFrame() {
        if (isFinished || firstFrameAt != null) return
        firstFrameAt = SystemClock.uptimeMillis()
        if (fullyDrawnAt != null) finish()
    }

    private fun onFullyDrawn() {
        if (isFinished || fullyDrawnAt != null) return
        fullyDrawnAt = SystemClock.uptimeMillis()
        if (firstFrameAt != null) finish()
    }

    /** Шлёт замер, если успел случиться первый кадр; без TTFD — если главная не дорисовалась. */
    private fun finish() {
        if (isFinished) return
        val ui = ui
        val firstFrameAt = firstFrameAt
        release()
        if (ui == null || firstFrameAt == null) return

        val activityManager = context.getSystemService(ActivityManager::class.java)
        val startInfo = activityManager.lastProcessStartInfo()
        analytics.eventAppStartup(
            StartupMetrics(
                ui = ui,
                entry = entry ?: StartupEntry.OTHER,
                appOnCreateMs = onCreateFinishedAt - onCreateStartedAt,
                ttidMs = firstFrameAt - processStartedAt,
                ttfdMs = fullyDrawnAt?.let { it - processStartedAt },
                ramGb = activityManager.totalRamGb(),
                apiLevel = Build.VERSION.SDK_INT,
                startType = startInfo.startTypeName(),
                startReason = startInfo.startReasonName(),
            ),
        )
    }

    private fun release() {
        isFinished = true
        handler.removeCallbacks(timeout)
        application?.unregisterActivityLifecycleCallbacks(this)
        application = null
    }

    private fun Activity.isMainActivity(): Boolean = this is MobileActivity || this is TvActivity

    /**
     * Момент первого кадра: onDraw — ещё до вывода на экран, поэтому время берём в сообщении,
     * поставленном в начало очереди сразу за кадром. Снимать листенер внутри onDraw нельзя.
     */
    private inner class FirstDrawListener(
        private val view: View,
    ) : ViewTreeObserver.OnDrawListener {
        private var isDrawn = false

        override fun onDraw() {
            if (isDrawn) return
            isDrawn = true
            handler.postAtFrontOfQueue { onFirstFrame() }
            handler.post { view.viewTreeObserver.removeOnDrawListener(this) }
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val TIMEOUT_MS = 30_000L
        const val MAX_ACTIVITY_DELAY_MS = 5_000L
    }
}
