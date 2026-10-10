package su.afk.yummy.tv.feature.player.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.common.model.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.presentation.R

/** Заголовок промпта следующей серии: отсчёт, переход в другую озвучку или обычный вопрос. */
@Composable
fun playerNextEpisodePromptTitle(
    prompt: PlayerEndPromptState,
    hasNextEpisode: Boolean,
    nextEpisodeDubbing: String?,
): String = when (prompt) {
    is PlayerEndPromptState.WithCountdown -> stringResource(
        R.string.player_next_episode_prompt_countdown,
        prompt.seconds,
    )

    else -> if (!hasNextEpisode && nextEpisodeDubbing != null) {
        stringResource(R.string.player_next_episode_prompt_other_dubbing, nextEpisodeDubbing)
    } else {
        stringResource(R.string.player_next_episode_prompt)
    }
}

/** Заголовок промпта финального действия: подписка на уведомления или оценка тайтла. */
@Composable
fun playerFinalEpisodePromptTitle(action: PlayerFinalEpisodeAction?): String = stringResource(
    if (action == PlayerFinalEpisodeAction.ManageSubscriptions) {
        R.string.player_notifications_prompt
    } else {
        R.string.player_rate_title_prompt
    },
)

/** Подпись основной кнопки промпта финального действия. */
@Composable
fun playerFinalEpisodePrimaryLabel(action: PlayerFinalEpisodeAction?): String = stringResource(
    if (action == PlayerFinalEpisodeAction.ManageSubscriptions) {
        R.string.player_manage_notifications
    } else {
        R.string.player_rate_title
    },
)
