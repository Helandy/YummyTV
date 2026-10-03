package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.model.AllohaTrackPreference
import su.afk.yummy.tv.domain.player.repository.AllohaTrackPreferenceRepository
import javax.inject.Inject

/** Запоминает выбор субтитров Alloha (или их отключение), не трогая ранее запомненную аудиодорожку. */
class SaveAllohaSubtitleSelectionUseCase @Inject constructor(
    private val repository: AllohaTrackPreferenceRepository,
) {
    suspend operator fun invoke(
        animeId: Int,
        dubbing: String,
        player: String,
        subtitleLanguage: String?,
        subtitleLabel: String?,
        subtitleOff: Boolean,
    ) {
        if (animeId <= 0 || dubbing.isBlank() || player.isBlank()) return
        val existing = repository.get(animeId, dubbing, player)
        repository.save(
            AllohaTrackPreference(
                animeId = animeId,
                dubbing = dubbing,
                player = player,
                audioLabel = existing?.audioLabel,
                subtitleLanguage = subtitleLanguage,
                subtitleLabel = subtitleLabel,
                subtitleOff = subtitleOff,
            ),
        )
    }
}
