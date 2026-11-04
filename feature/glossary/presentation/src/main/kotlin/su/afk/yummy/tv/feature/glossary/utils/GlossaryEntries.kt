package su.afk.yummy.tv.feature.glossary.utils

import su.afk.yummy.tv.feature.glossary.model.GlossaryCategory
import su.afk.yummy.tv.feature.glossary.model.GlossaryEntry
import su.afk.yummy.tv.feature.glossary.presentation.R

/** Каталог терминов глоссария; тексты лежат в ресурсах по ключу `glossary_<key>_*`. */
val GLOSSARY_ENTRIES: List<GlossaryEntry> = listOf(
    GlossaryEntry("title", GlossaryCategory.SLANG, R.string.glossary_title_term, R.string.glossary_title_aliases, R.string.glossary_title_definition),
    GlossaryEntry("ongoing", GlossaryCategory.SLANG, R.string.glossary_ongoing_term, R.string.glossary_ongoing_aliases, R.string.glossary_ongoing_definition),
    GlossaryEntry("fanservice", GlossaryCategory.SLANG, R.string.glossary_fanservice_term, R.string.glossary_fanservice_aliases, R.string.glossary_fanservice_definition),
    GlossaryEntry("waifu", GlossaryCategory.SLANG, R.string.glossary_waifu_term, R.string.glossary_waifu_aliases, R.string.glossary_waifu_definition),
    GlossaryEntry("manga", GlossaryCategory.GENRES, R.string.glossary_manga_term, R.string.glossary_manga_aliases, R.string.glossary_manga_definition),
    GlossaryEntry("ranobe", GlossaryCategory.GENRES, R.string.glossary_ranobe_term, R.string.glossary_ranobe_aliases, R.string.glossary_ranobe_definition),
    GlossaryEntry("isekai", GlossaryCategory.GENRES, R.string.glossary_isekai_term, R.string.glossary_isekai_aliases, R.string.glossary_isekai_definition),
    GlossaryEntry("shounen", GlossaryCategory.GENRES, R.string.glossary_shounen_term, R.string.glossary_shounen_aliases, R.string.glossary_shounen_definition),
    GlossaryEntry("shoujo", GlossaryCategory.GENRES, R.string.glossary_shoujo_term, R.string.glossary_shoujo_aliases, R.string.glossary_shoujo_definition),
    GlossaryEntry("seinen", GlossaryCategory.GENRES, R.string.glossary_seinen_term, R.string.glossary_seinen_aliases, R.string.glossary_seinen_definition),
    GlossaryEntry("mecha", GlossaryCategory.GENRES, R.string.glossary_mecha_term, R.string.glossary_mecha_aliases, R.string.glossary_mecha_definition),
    GlossaryEntry("slice", GlossaryCategory.GENRES, R.string.glossary_slice_term, R.string.glossary_slice_aliases, R.string.glossary_slice_definition),
    GlossaryEntry("ova", GlossaryCategory.GENRES, R.string.glossary_ova_term, R.string.glossary_ova_aliases, R.string.glossary_ova_definition),
    GlossaryEntry("ona", GlossaryCategory.GENRES, R.string.glossary_ona_term, R.string.glossary_ona_aliases, R.string.glossary_ona_definition),
    GlossaryEntry("special", GlossaryCategory.GENRES, R.string.glossary_special_term, R.string.glossary_special_aliases, R.string.glossary_special_definition),
    GlossaryEntry("source", GlossaryCategory.PRODUCTION, R.string.glossary_source_term, R.string.glossary_source_aliases, R.string.glossary_source_definition),
    GlossaryEntry("season", GlossaryCategory.PRODUCTION, R.string.glossary_season_term, R.string.glossary_season_aliases, R.string.glossary_season_definition),
    GlossaryEntry("arc", GlossaryCategory.PRODUCTION, R.string.glossary_arc_term, R.string.glossary_arc_aliases, R.string.glossary_arc_definition),
    GlossaryEntry("filler", GlossaryCategory.PRODUCTION, R.string.glossary_filler_term, R.string.glossary_filler_aliases, R.string.glossary_filler_definition),
    GlossaryEntry("recap", GlossaryCategory.PRODUCTION, R.string.glossary_recap_term, R.string.glossary_recap_aliases, R.string.glossary_recap_definition),
    GlossaryEntry("opening", GlossaryCategory.PRODUCTION, R.string.glossary_opening_term, R.string.glossary_opening_aliases, R.string.glossary_opening_definition),
    GlossaryEntry("studio", GlossaryCategory.PRODUCTION, R.string.glossary_studio_term, R.string.glossary_studio_aliases, R.string.glossary_studio_definition),
    GlossaryEntry("dub", GlossaryCategory.VOICE, R.string.glossary_dub_term, R.string.glossary_dub_aliases, R.string.glossary_dub_definition),
    GlossaryEntry("voiceover", GlossaryCategory.VOICE, R.string.glossary_voiceover_term, R.string.glossary_voiceover_aliases, R.string.glossary_voiceover_definition),
    GlossaryEntry("fandub", GlossaryCategory.VOICE, R.string.glossary_fandub_term, R.string.glossary_fandub_aliases, R.string.glossary_fandub_definition),
    GlossaryEntry("seiyuu", GlossaryCategory.VOICE, R.string.glossary_seiyuu_term, R.string.glossary_seiyuu_aliases, R.string.glossary_seiyuu_definition),
    GlossaryEntry("subs", GlossaryCategory.VOICE, R.string.glossary_subs_term, R.string.glossary_subs_aliases, R.string.glossary_subs_definition),
    GlossaryEntry("hardsub", GlossaryCategory.VOICE, R.string.glossary_hardsub_term, R.string.glossary_hardsub_aliases, R.string.glossary_hardsub_definition),
    GlossaryEntry("softsub", GlossaryCategory.VOICE, R.string.glossary_softsub_term, R.string.glossary_softsub_aliases, R.string.glossary_softsub_definition),
    GlossaryEntry("raw", GlossaryCategory.VOICE, R.string.glossary_raw_term, R.string.glossary_raw_aliases, R.string.glossary_raw_definition),
    GlossaryEntry("player", GlossaryCategory.VOICE, R.string.glossary_player_term, R.string.glossary_player_aliases, R.string.glossary_player_definition),
)
