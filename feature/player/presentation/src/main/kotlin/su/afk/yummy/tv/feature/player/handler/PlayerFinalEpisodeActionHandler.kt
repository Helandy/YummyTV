package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.utils.isOngoingAnimeStatus
import javax.inject.Inject

internal class PlayerFinalEpisodeActionHandler @Inject constructor(
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAccountSession: GetAccountSessionUseCase,
) {
    suspend fun resolve(animeId: Int): PlayerFinalEpisodeAction {
        if (animeId <= 0) return PlayerFinalEpisodeAction.None

        val details = try {
            getAnimeDetails(animeId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return PlayerFinalEpisodeAction.None
        }
        if (!details.status.isOngoingAnimeStatus()) {
            return PlayerFinalEpisodeAction.RateTitle
        }

        val session = try {
            getAccountSession()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return PlayerFinalEpisodeAction.None
        }
        return if (session.isAuthorized && session.userId > 0) {
            PlayerFinalEpisodeAction.ManageSubscriptions
        } else {
            PlayerFinalEpisodeAction.None
        }
    }
}
