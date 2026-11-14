package su.afk.yummy.tv.feature.player.mapper

import androidx.annotation.StringRes
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.feature.player.presentation.R

/** Текст шага на экране ожидания потока. */
@StringRes
fun PlayerStreamResolveStatus?.loadingTextRes(): Int = when (this) {
    null, PlayerStreamResolveStatus.Preparing -> R.string.player_loading_stream
    PlayerStreamResolveStatus.FetchingPage -> R.string.player_resolve_fetching_page
    PlayerStreamResolveStatus.ParsingPage -> R.string.player_resolve_parsing_page
    PlayerStreamResolveStatus.RequestingStream -> R.string.player_resolve_requesting_stream
    PlayerStreamResolveStatus.CheckingQualities -> R.string.player_resolve_checking_qualities
    PlayerStreamResolveStatus.OpeningSession -> R.string.player_resolve_opening_session
    PlayerStreamResolveStatus.Retrying -> R.string.player_resolve_retrying
    PlayerStreamResolveStatus.ResponseError -> R.string.player_resolve_response_error
    PlayerStreamResolveStatus.LoadingPlayerScript -> R.string.player_resolve_loading_player_script
    PlayerStreamResolveStatus.SelectingDubbing -> R.string.player_resolve_selecting_dubbing
    PlayerStreamResolveStatus.DecodingLinks -> R.string.player_resolve_decoding_links
    PlayerStreamResolveStatus.SearchingEpisode -> R.string.player_resolve_searching_episode
    PlayerStreamResolveStatus.ExtractingLink -> R.string.player_resolve_extracting_link
    PlayerStreamResolveStatus.LoadingPlaylist -> R.string.player_resolve_loading_playlist
    PlayerStreamResolveStatus.EpisodeNotFound -> R.string.player_problem_episode_not_found
    PlayerStreamResolveStatus.PlayerNotFound -> R.string.player_problem_player_not_found
    PlayerStreamResolveStatus.LinkNotFound -> R.string.player_problem_link_not_found
    PlayerStreamResolveStatus.WaitingForAd -> R.string.player_resolve_waiting_for_ad
    PlayerStreamResolveStatus.WaitingForTorrent -> R.string.player_resolve_waiting_for_torrent
    PlayerStreamResolveStatus.WaitingForTorrentFile -> R.string.player_resolve_waiting_for_torrent_file
}

/** Строка ошибки: на каком шаге оборвалось получение потока. */
@StringRes
fun PlayerStreamResolveStatus.failedStepTextRes(): Int = when (this) {
    PlayerStreamResolveStatus.Preparing,
    PlayerStreamResolveStatus.FetchingPage,
    -> R.string.player_resolve_failed_fetching_page
    PlayerStreamResolveStatus.ParsingPage -> R.string.player_resolve_failed_parsing_page
    PlayerStreamResolveStatus.RequestingStream,
    PlayerStreamResolveStatus.Retrying,
    PlayerStreamResolveStatus.WaitingForAd,
    PlayerStreamResolveStatus.WaitingForTorrent,
    PlayerStreamResolveStatus.WaitingForTorrentFile,
    -> R.string.player_resolve_failed_requesting_stream
    PlayerStreamResolveStatus.ResponseError -> R.string.player_resolve_failed_response_error
    PlayerStreamResolveStatus.CheckingQualities -> R.string.player_resolve_failed_checking_qualities
    PlayerStreamResolveStatus.LoadingPlayerScript -> R.string.player_resolve_failed_loading_player_script
    PlayerStreamResolveStatus.SelectingDubbing -> R.string.player_resolve_failed_selecting_dubbing
    PlayerStreamResolveStatus.DecodingLinks -> R.string.player_resolve_failed_decoding_links
    PlayerStreamResolveStatus.SearchingEpisode -> R.string.player_problem_episode_not_found
    PlayerStreamResolveStatus.ExtractingLink -> R.string.player_resolve_failed_extracting_link
    PlayerStreamResolveStatus.LoadingPlaylist -> R.string.player_resolve_failed_loading_playlist
    PlayerStreamResolveStatus.EpisodeNotFound -> R.string.player_problem_episode_not_found
    PlayerStreamResolveStatus.PlayerNotFound -> R.string.player_problem_player_not_found
    PlayerStreamResolveStatus.LinkNotFound -> R.string.player_problem_link_not_found
    PlayerStreamResolveStatus.OpeningSession -> R.string.player_resolve_failed_opening_session
}
