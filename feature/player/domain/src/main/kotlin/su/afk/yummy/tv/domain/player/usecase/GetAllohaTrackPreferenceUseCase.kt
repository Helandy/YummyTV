package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.model.AllohaTrackPreference
import su.afk.yummy.tv.domain.player.repository.AllohaTrackPreferenceRepository
import javax.inject.Inject

/** Возвращает запомненный выбор аудиодорожки и субтитров Alloha для озвучки тайтла или null. */
class GetAllohaTrackPreferenceUseCase @Inject constructor(
    private val repository: AllohaTrackPreferenceRepository,
) {
    suspend operator fun invoke(animeId: Int, dubbing: String, player: String): AllohaTrackPreference? {
        if (animeId <= 0 || dubbing.isBlank() || player.isBlank()) return null
        return repository.get(animeId, dubbing, player)
    }
}
