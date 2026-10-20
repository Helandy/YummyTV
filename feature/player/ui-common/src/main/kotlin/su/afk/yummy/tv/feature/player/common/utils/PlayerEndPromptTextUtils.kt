package su.afk.yummy.tv.feature.player.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.common.model.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeUnavailable
import su.afk.yummy.tv.feature.player.presentation.R

/** Заголовок промпта следующей серии: с отсчётом или обычный вопрос. */
@Composable
fun playerNextEpisodePromptTitle(prompt: PlayerEndPromptState): String = when (prompt) {
    is PlayerEndPromptState.WithCountdown -> stringResource(
        R.string.player_next_episode_prompt_countdown,
        prompt.seconds,
    )

    else -> stringResource(R.string.player_next_episode_prompt)
}

/**
 * Заголовок промпта финального действия. Подписка предлагается, когда следующей серии нет нигде, —
 * текст тот же, что в диалоге по кнопке «следующая»: «Серии N пока нет…» + вопрос второй строкой.
 */
@Composable
fun playerFinalEpisodePromptTitle(action: PlayerFinalEpisodeAction?, activeEpisode: String): String =
    if (action == PlayerFinalEpisodeAction.ManageSubscriptions) {
        playerNextEpisodeNotReleasedTitle(activeEpisode.toIntOrNull()?.plus(1))
    } else {
        stringResource(R.string.player_rate_title_prompt)
    }

/** Вторая строка промпта финального действия: вопрос о подписке. */
@Composable
fun playerFinalEpisodePromptSubtitle(action: PlayerFinalEpisodeAction?): String? =
    if (action == PlayerFinalEpisodeAction.ManageSubscriptions) {
        stringResource(R.string.player_next_episode_subscribe_prompt)
    } else {
        null
    }

@Composable
private fun playerNextEpisodeNotReleasedTitle(episodeNumber: Int?): String =
    if (episodeNumber != null) {
        stringResource(R.string.player_next_episode_not_released, episodeNumber)
    } else {
        stringResource(R.string.player_next_episode_not_released_unknown)
    }

/** Подпись основной кнопки промпта финального действия. */
@Composable
fun playerFinalEpisodePrimaryLabel(action: PlayerFinalEpisodeAction?): String = stringResource(
    if (action == PlayerFinalEpisodeAction.ManageSubscriptions) {
        R.string.player_manage_notifications
    } else {
        R.string.player_rate_title
    },
)

/** Серию N+1 можно посмотреть в другой озвучке или на другом балансере. */
val PlayerNextEpisodeUnavailable.hasOtherSource: Boolean
    get() = options.isNotEmpty()

/** Заголовок диалога «в этом плеере следующей серии нет». */
@Composable
fun playerNextEpisodeUnavailableTitle(info: PlayerNextEpisodeUnavailable): String {
    val episodeNumber = info.episodeNumber
    return when {
        info.hasOtherSource && episodeNumber != null && info.inCurrentDubbing -> stringResource(
            R.string.player_next_episode_missing_in_player,
            episodeNumber,
        )

        info.hasOtherSource && episodeNumber != null -> stringResource(
            R.string.player_next_episode_missing_in_dubbing,
            info.currentDubbing,
            episodeNumber,
        )

        else -> playerNextEpisodeNotReleasedTitle(episodeNumber)
    }
}

/** Вторая строка заголовка: где искать серию, либо вопрос о подписке. */
@Composable
fun playerNextEpisodeUnavailableSubtitle(info: PlayerNextEpisodeUnavailable): String? = when {
    !info.hasOtherSource -> if (info.canSubscribe) {
        stringResource(R.string.player_next_episode_subscribe_prompt)
    } else {
        null
    }

    info.inCurrentDubbing -> stringResource(R.string.player_next_episode_available_here)
    else -> stringResource(R.string.player_next_episode_available_in_other_dubbings)
}

/** Основная кнопка диалога без списка источников: подписка на новые серии либо просто закрыть. */
@Composable
fun playerNextEpisodeUnavailablePrimaryLabel(info: PlayerNextEpisodeUnavailable): String = stringResource(
    if (info.canSubscribe) R.string.player_manage_notifications else R.string.player_next_episode_dialog_ok,
)

/** Вторичная кнопка диалога без списка источников есть, только когда основная что-то делает. */
@Composable
fun playerNextEpisodeUnavailableSecondaryLabel(info: PlayerNextEpisodeUnavailable): String? =
    if (info.canSubscribe) stringResource(R.string.player_next_episode_dialog_cancel) else null
