package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.model.AllohaTrackPreference
import su.afk.yummy.tv.domain.player.repository.AllohaTrackPreferenceRepository
import javax.inject.Inject

/**
 * Запоминает выбор аудиодорожки Alloha, не трогая ранее запомненный выбор субтитров: хранилище
 * держит оба выбора в одной записи на (animeId, dubbing, player), поэтому запись идёт поверх
 * существующей, а не вместо неё.
 */
class SaveAllohaAudioSelectionUseCase @Inject constructor(
    private val repository: AllohaTrackPreferenceRepository,
) {
    suspend operator fun invoke(animeId: Int, dubbing: String, player: String, audioLabel: String) {
        if (animeId <= 0 || dubbing.isBlank() || player.isBlank()) return
        val existing = repository.get(animeId, dubbing, player)
        repository.save(
            AllohaTrackPreference(
                animeId = animeId,
                dubbing = dubbing,
                player = player,
                audioLabel = audioLabel,
                subtitleLanguage = existing?.subtitleLanguage,
                subtitleLabel = existing?.subtitleLabel,
                subtitleOff = existing?.subtitleOff ?: false,
            ),
        )
    }
}
