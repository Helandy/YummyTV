package su.afk.yummy.tv.feature.player.handler

import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack
import su.afk.yummy.tv.domain.player.usecase.GetAllohaTrackPreferenceUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveAllohaAudioSelectionUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveAllohaSubtitleSelectionUseCase
import javax.inject.Inject

/**
 * Найденное совпадение сохранённого выбора с текущим (заново распарсенным) списком дорожек Alloha.
 *
 * [applySubtitleChange] отличает "пользователь явно выключил субтитры или выбрал дорожку, которая
 * нашлась" (true, [subtitleIndex] — итоговый индекс или null для "выключено") от "сохранённая
 * дорожка есть, но среди текущих субтитров такой больше нет" (false — оставляем дефолт экстрактора).
 */
internal data class AllohaTrackMatch(
    val audioId: String?,
    val applySubtitleChange: Boolean,
    val subtitleIndex: Int?,
)

/**
 * Запоминает и восстанавливает выбор аудиодорожки/субтитров Alloha-плеера для озвучки тайтла.
 *
 * `id` аудиодорожки и `url` субтитров генерируются заново при каждом парсинге BSIN, поэтому
 * сопоставление со старым выбором идёт по стабильным полям — `label` дорожки и `language`/`label`
 * субтитров, а не по id/url напрямую.
 */
internal class PlayerAllohaTrackPreferenceHandler @Inject constructor(
    private val getPreference: GetAllohaTrackPreferenceUseCase,
    private val saveAudio: SaveAllohaAudioSelectionUseCase,
    private val saveSubtitle: SaveAllohaSubtitleSelectionUseCase,
) {

    /** Возвращает найденное совпадение или null, если сохранённого выбора нет либо он не совпал. */
    suspend fun findMatch(
        animeId: Int,
        dubbing: String,
        player: String,
        audioTracks: List<AllohaAudioTrack>,
        subtitles: List<AllohaSubtitleTrack>,
    ): AllohaTrackMatch? {
        val preference = getPreference(animeId, dubbing, player) ?: return null

        val audioId = preference.audioLabel?.let { label ->
            // Несколько дорожек с одинаковым label — неоднозначность, лучше не гадать.
            audioTracks.singleOrNull { it.label.equals(label, ignoreCase = true) }
        }?.id

        val applySubtitleChange: Boolean
        val subtitleIndex: Int?
        if (preference.subtitleOff) {
            applySubtitleChange = true
            subtitleIndex = null
        } else {
            val index =
                matchSubtitleIndex(preference.subtitleLabel, preference.subtitleLanguage, subtitles)
            applySubtitleChange = index != null
            subtitleIndex = index
        }

        return AllohaTrackMatch(
            audioId = audioId,
            applySubtitleChange = applySubtitleChange,
            subtitleIndex = subtitleIndex,
        )
    }

    /**
     * У Alloha нередко несколько субтитров имеют одинаковый [AllohaSubtitleTrack.language]
     * (например `"ru"`), различаясь только [AllohaSubtitleTrack.label] (разные источники/фансаб).
     * Поэтому матчим по убыванию точности вместо одного OR-условия: сперва точное совпадение
     * label+language вместе, затем только по label, и лишь если это не помогло — по языку, но
     * только когда он однозначно указывает на единственную дорожку в списке.
     */
    private fun matchSubtitleIndex(
        label: String?,
        language: String?,
        subtitles: List<AllohaSubtitleTrack>,
    ): Int? {
        if (label != null && language != null) {
            val composite = subtitles.indexOfFirst {
                it.label.equals(label, ignoreCase = true) && it.language.equals(
                    language,
                    ignoreCase = true
                )
            }
            if (composite >= 0) return composite
        }
        if (label != null) {
            val byLabel = subtitles.indexOfFirst { it.label.equals(label, ignoreCase = true) }
            if (byLabel >= 0) return byLabel
        }
        if (language != null) {
            val candidates = subtitles.withIndex()
                .filter { (_, subtitle) -> subtitle.language.equals(language, ignoreCase = true) }
            if (candidates.size == 1) return candidates.single().index
        }
        return null
    }

    /** Сохраняет выбор аудиодорожки, не трогая ранее запомненный выбор субтитров. */
    suspend fun saveAudioSelection(
        animeId: Int,
        dubbing: String,
        player: String,
        audioLabel: String,
    ) = saveAudio(animeId, dubbing, player, audioLabel)

    /** Сохраняет выбор субтитров (или их отключение), не трогая ранее запомненный выбор аудиодорожки. */
    suspend fun saveSubtitleSelection(
        animeId: Int,
        dubbing: String,
        player: String,
        subtitleLanguage: String?,
        subtitleLabel: String?,
        subtitleOff: Boolean,
    ) = saveSubtitle(animeId, dubbing, player, subtitleLanguage, subtitleLabel, subtitleOff)
}
