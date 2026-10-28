package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import su.afk.yummy.tv.feature.player.PlayerSkips

@Immutable
data class PlayerPlaybackUiState(
    val activeIframeUrl: String,
    val activeEpisode: String,
    val activeVideoId: Int,
    val activeDubbing: String,
    val activeBalancerName: String,
    /** Имя активного балансера для показа, без префикса «Плеер» — как в [balancerNames]. */
    val activeBalancerLabel: String,
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
    /** Качеством управляет адаптивный режим: включён в настройках и в серии не выбрано вручную. */
    val adaptiveQuality: Boolean,
    /** Выше этой высоты адаптивный режим качество не поднимает. */
    val adaptiveCeilingHeight: Int,
    /** Метка потока, выше которой адаптация не поднимается (лимит, показываемый пользователю). */
    val adaptiveLimitLabel: String?,
    /** Лимит адаптации снят («Лучшее»): ползунок показывает последний пункт. */
    val adaptiveLimitBest: Boolean,
    /** URL, который реально проигрывается: выбранное качество или исходный стрим. */
    val playbackUrl: String,
    /** Балансер для заголовка «X вернул ошибку»; null, если ошибка не от балансера (офлайн, нет имени). */
    val errorBalancerLabel: String?,
    val canChangePlayer: Boolean,
    val canChangeDubbing: Boolean,
    /** Восстановление затянулось и есть куда переключиться — показать «Сменить плеер/озвучку». */
    val showRecoveryHint: Boolean,
    /** Позиция, с которой продолжить после смены источника с экрана ошибки. */
    val errorResumePositionMs: Long,
)
