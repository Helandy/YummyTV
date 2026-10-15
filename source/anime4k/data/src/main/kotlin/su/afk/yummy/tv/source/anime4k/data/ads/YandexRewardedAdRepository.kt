package su.afk.yummy.tv.source.anime4k.data.ads

import android.app.Activity
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.utils.system.ForegroundAdGate
import su.afk.yummy.tv.source.anime4k.data.BuildConfig
import su.afk.yummy.tv.source.anime4k.domain.repository.RewardedAdRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Rewarded-реклама Яндекса. Серверной верификации у SDK нет: награда это клиентский `onRewarded`.
 * Любой сбой (нет Activity, не загрузилась, показ упал) означает `false`: ссылок без рекламы не будет.
 * В debug-сборке реклама отключена: согласие и награда выдаются сразу.
 */
@Singleton
internal class YandexRewardedAdRepository @Inject constructor() : RewardedAdRepository {

    private var initialized = false

    /** Момент согласия, выданного до перехода в плеер; расходуется первым же [askConsent]. */
    @Volatile
    private var preOpenConsentAtMs = 0L

    override val isAvailable: Boolean
        get() = BuildConfig.DEBUG || BuildConfig.YANDEX_REWARDED_AD_UNIT_ID.isNotBlank()

    override suspend fun askConsent(): Boolean {
        val grantedAt = preOpenConsentAtMs
        preOpenConsentAtMs = 0L
        if (grantedAt != 0L && System.currentTimeMillis() - grantedAt < PRE_OPEN_CONSENT_TTL_MS) return true
        return ask()
    }

    override suspend fun confirmBeforeOpen(): Boolean {
        val granted = ask()
        preOpenConsentAtMs = if (granted) System.currentTimeMillis() else 0L
        return granted
    }

    private suspend fun ask(): Boolean = withContext(Dispatchers.Main) {
        // В debug рекламу не показываем: ни вопрос о согласии, ни сам ролик.
        if (BuildConfig.DEBUG) return@withContext true
        if (!isAvailable) return@withContext false
        val activity = CurrentActivityTracker.current() ?: return@withContext false
        AdConsentDialog.ask(activity)
    }

    override suspend fun showRewardedAd(): Boolean = withContext(Dispatchers.Main) {
        if (BuildConfig.DEBUG) return@withContext true
        if (!isAvailable) return@withContext false
        val activity = CurrentActivityTracker.current() ?: return@withContext false
        ensureInitialized(activity)
        val ad = withTimeoutOrNull(LOAD_TIMEOUT_MS) { load(activity) } ?: return@withContext false
        ForegroundAdGate.around { show(activity, ad) }
    }

    private fun ensureInitialized(activity: Activity) {
        if (initialized) return
        YandexAds.initialize(activity.applicationContext) { }
        initialized = true
    }

    private suspend fun load(activity: Activity): RewardedAd? = suspendCancellableCoroutine { continuation ->
        val loader = RewardedAdLoader(activity)
        continuation.invokeOnCancellation { loader.cancelLoading() }
        loader.loadAd(
            AdRequest.Builder(BuildConfig.YANDEX_REWARDED_AD_UNIT_ID).build(),
            object : RewardedAdLoadListener {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    if (continuation.isActive) continuation.resume(rewardedAd)
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    if (continuation.isActive) continuation.resume(null)
                }
            },
        )
    }

    private suspend fun show(activity: Activity, ad: RewardedAd): Boolean = suspendCancellableCoroutine { continuation ->
        var rewarded = false
        ad.setAdEventListener(
            object : RewardedAdEventListener {
                override fun onAdShown() = Unit

                override fun onAdFailedToShow(adError: AdError) {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onAdDismissed() {
                    if (continuation.isActive) continuation.resume(rewarded)
                }

                override fun onAdClicked() = Unit

                override fun onAdImpression(impressionData: ImpressionData?) = Unit

                override fun onRewarded(reward: Reward) {
                    rewarded = true
                }
            },
        )
        ad.show(activity)
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 15_000L
        const val PRE_OPEN_CONSENT_TTL_MS = 30_000L
    }
}
