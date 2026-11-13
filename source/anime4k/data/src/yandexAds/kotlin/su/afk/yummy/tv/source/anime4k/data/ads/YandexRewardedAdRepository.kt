package su.afk.yummy.tv.source.anime4k.data.ads

import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.reportResolveStep
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
import su.afk.yummy.tv.core.common.player.ResolveProgress
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import su.afk.yummy.tv.core.utils.system.ForegroundAdGate
import su.afk.yummy.tv.source.anime4k.data.BuildConfig
import su.afk.yummy.tv.source.anime4k.data.flag.Anime4kFeatureFlag
import su.afk.yummy.tv.source.anime4k.domain.repository.RewardedAdRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

/**
 * Rewarded-реклама Яндекса. Серверной верификации у SDK нет: награда это клиентский `onRewarded`.
 * Любой сбой (нет Activity, не загрузилась, показ упал) означает `false`: ссылок без рекламы не будет.
 * В debug-сборке или при выключенном флаге `anime4k_ads_enabled` реклама пропускается: согласие и награда выдаются сразу.
 * Id блока берётся из флага `anime4k_yandex_rewarded_ad_unit_id`, при пустом — из BuildConfig.
 */
@Singleton
internal class YandexRewardedAdRepository @Inject constructor(
    private val featureToggleProvider: FeatureToggleProvider,
) : RewardedAdRepository {

    private var initialized = false

    /** Момент согласия, выданного до перехода в плеер; расходуется первым же [askConsent]. */
    @Volatile
    private var preOpenConsentAtMs = 0L

    private val adsBypassed: Boolean
        get() = BuildConfig.DEBUG || !featureToggleProvider.isEnabled(Anime4kFeatureFlag.adsEnabled)

    override val isAvailable: Boolean
        get() = adsBypassed || adUnitId().isNotBlank()

    /** Id блока из Varioqub; пустой — локальный из `local.properties`. */
    private fun adUnitId(): String =
        featureToggleProvider.getString(Anime4kFeatureFlag.rewardedAdUnitId).trim()
            .ifBlank { BuildConfig.YANDEX_REWARDED_AD_UNIT_ID }

    override suspend fun askConsent(): Boolean {
        reportResolveStep(PlayerStreamResolveStatus.AskingConsent)
        val grantedAt = preOpenConsentAtMs
        preOpenConsentAtMs = 0L
        return (grantedAt != 0L && System.currentTimeMillis() - grantedAt < PRE_OPEN_CONSENT_TTL_MS) || ask()
    }

    override suspend fun confirmBeforeOpen(): Boolean {
        val granted = ask()
        preOpenConsentAtMs = if (granted) System.currentTimeMillis() else 0L
        return granted
    }

    private suspend fun ask(): Boolean = withContext(Dispatchers.Main) {
        if (adsBypassed) return@withContext true
        if (!isAvailable) return@withContext false
        val activity = CurrentActivityTracker.current() ?: return@withContext false
        AdConsentDialog.ask(activity)
    }

    override suspend fun showRewardedAd(): Boolean = withContext(Dispatchers.Main) {
        coroutineContext[ResolveProgress]?.report(PlayerStreamResolveStatus.WaitingForAd)
        if (adsBypassed) return@withContext true
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
            AdRequest.Builder(adUnitId()).build(),
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
