package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.utils.activeBalancer
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbing
import su.afk.yummy.tv.feature.player.utils.activeDubbingEpisodes
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeEpisodeSource
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import su.afk.yummy.tv.feature.player.utils.activeQuality
import su.afk.yummy.tv.feature.player.utils.activeScreenshotUrl
import su.afk.yummy.tv.feature.player.utils.activeVideoId
import su.afk.yummy.tv.feature.player.utils.displayedBalancerIndices
import su.afk.yummy.tv.feature.player.utils.globalDubbingEpisodeNumbers
import su.afk.yummy.tv.feature.player.utils.globalDubbingNames
import su.afk.yummy.tv.feature.player.utils.globalDubbingSourceNames
import su.afk.yummy.tv.feature.player.utils.globalDubbingViews
import su.afk.yummy.tv.feature.player.utils.isBalancerAvailableForEpisode
import su.afk.yummy.tv.feature.player.utils.isDubbingAvailableForEpisode
import su.afk.yummy.tv.feature.player.utils.isFinalAvailableEpisode
import su.afk.yummy.tv.feature.player.utils.nextEpisodeOtherDubbingSource
import su.afk.yummy.tv.feature.player.utils.normalizedSourceSelection
import su.afk.yummy.tv.feature.player.utils.streamQualities

@Immutable
data class PlayerPlaybackUiState(
    val activeIframeUrl: String,
    val activeEpisode: String,
    val activeVideoId: Int,
    val activeDubbing: String,
    val activeBalancerName: String,
    val activeScreenshotUrl: String,
    val activeSkips: PlayerSkips,
    val hasPrevEpisode: Boolean,
    val hasNextEpisode: Boolean,
    /** Озвучка, в которой есть серия N+1, когда в активной озвучке серии закончились */
    val nextEpisodeDubbing: String?,
    val finalEpisodeAction: PlayerFinalEpisodeAction,
    val dubbingNames: ImmutableList<String>,
    val dubbingEpisodeCounts: ImmutableList<Int>,
    val dubbingViews: ImmutableList<Int>,
    val dubbingSourceNames: ImmutableList<String>,
    val dubbingAvailability: ImmutableList<Boolean>,
    val currentDubbingIndex: Int,
    /** Имена балансеров для показа, уже без префикса «Плеер». */
    val balancerNames: ImmutableList<String>,
    val balancerAvailability: ImmutableList<Boolean>,
    val currentBalancerIndex: Int,
    /** Доступные качества стрима: от экстрактора или выведенные из URL. */
    val qualityLabels: ImmutableList<String>,
    /** Выбранное качество, а если его нет среди доступных — лучшее доступное. */
    val activeQuality: String?,
    /** URL, который реально проигрывается: выбранное качество или исходный стрим. */
    val playbackUrl: String,
    val canChangePlayer: Boolean,
    val canChangeDubbing: Boolean,
    /** Восстановление затянулось и есть куда переключиться — показать «Сменить плеер/озвучку». */
    val showRecoveryHint: Boolean,
    /** Позиция, с которой продолжить после смены источника с экрана ошибки. */
    val errorResumePositionMs: Long,
)
