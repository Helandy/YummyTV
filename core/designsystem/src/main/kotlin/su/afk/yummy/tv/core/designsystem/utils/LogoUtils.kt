package su.afk.yummy.tv.core.designsystem.utils

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.R

private val LOGO_LABEL_PREFIXES = listOf("плеер", "player", "озвучка", "субтитры", "дубляж")

private val DUBBING_LOGOS: Map<String, Int> = mapOf(
    "anistar" to R.drawable.dub_anistar,
    "animevost" to R.drawable.dub_animevost,
    "anilibria" to R.drawable.dub_anilibria,
    "animaunt" to R.drawable.dub_animaunt,
    "aniliberty" to R.drawable.dub_aniliberty,
    "mcentertainment" to R.drawable.dub_mc_entertainment,
    "kansai" to R.drawable.dub_kansai,
    "kansaistudio" to R.drawable.dub_kansai,
    "reanimedia" to R.drawable.dub_reanimedia,
    "hdrezkastudio" to R.drawable.dub_hdrezka_studio,
    "anythinggroup" to R.drawable.dub_anything_group,
    "ancord" to R.drawable.dub_ancord,
    "ancordмногоголосый" to R.drawable.dub_ancord,
    "animedia" to R.drawable.dub_animedia,
    "amedia" to R.drawable.dub_animedia,
    "2x2" to R.drawable.dub_2x2,
    "2x2new" to R.drawable.dub_2x2,
    "свдубль" to R.drawable.dub_sv_double,
    "кладбищетоповыхрелизов" to R.drawable.dub_ktr,
    "anistardeep" to R.drawable.dub_anistar,
    "anistarмногоголосый" to R.drawable.dub_anistar,
    "руанимеdeep" to R.drawable.dub_deep,
    "оканэ" to R.drawable.dub_okane,
    "anilibriaold" to R.drawable.dub_anilibria,
    "studiobandjunior" to R.drawable.dub_studioband,
    "victorия" to R.drawable.dub_victoriya,
    "onwave18" to R.drawable.dub_onwave,
    "ndastudio" to R.drawable.dub_nda_studio,
    "ndavoice" to R.drawable.dub_nda_studio,
    "svdouble" to R.drawable.dub_sv_double,
    "onibaku" to R.drawable.dub_onibaku,
    "wakanim" to R.drawable.dub_wakanim,
    "anifilm" to R.drawable.dub_anifilm,
    "persona99" to R.drawable.dub_persona99,
    "getsmartgroup" to R.drawable.dub_get_smart_group,
    "gsg" to R.drawable.dub_get_smart_group,
    "sovetromantica" to R.drawable.dub_sovetromantica,
    "coldfilm" to R.drawable.dub_coldfilm,
    "amber" to R.drawable.dub_amber,
    "studioamber" to R.drawable.dub_amber,
    "foxforce" to R.drawable.dub_foxforce,
    "panzuinfo" to R.drawable.dub_panzu_info,
    "ktr" to R.drawable.dub_ktr,
    "animejoy" to R.drawable.dub_animejoy,
    "anijoy" to R.drawable.dub_anijoy,
    "aniplague" to R.drawable.dub_aniplague,
    "anibomj" to R.drawable.dub_aniplague,
    "hora" to R.drawable.dub_hora,
    "horanoyume" to R.drawable.dub_hora,
    "rhs" to R.drawable.dub_rhs,
    "redheadsound" to R.drawable.dub_rhs,
    "tvshows" to R.drawable.dub_tvshows,
    "chomosukest" to R.drawable.dub_chomosukest,
    "sekaiproject" to R.drawable.dub_sekai_project,
    "beyondstudio" to R.drawable.dub_beyond_studio,
    "honkon" to R.drawable.dub_hon_kon,
    "honkonstudio" to R.drawable.dub_hon_kon,
    "anisense" to R.drawable.dub_anisense,
    "animereactor" to R.drawable.dub_animereactor,
    "anihope" to R.drawable.dub_anihope,
    "anileague" to R.drawable.dub_anileague,
    "aniclub" to R.drawable.dub_aniclub,
    "cozysound" to R.drawable.dub_cozysound,
    "dubclub" to R.drawable.dub_dubclub,
    "ldateam" to R.drawable.dub_lda_team,
    "phoenixdub" to R.drawable.dub_phoenixdub,
    "ryukendub" to R.drawable.dub_ryukendub,
    "studiinet" to R.drawable.dub_studii_net,
    "shinajistudio" to R.drawable.dub_shinajistudio,
    "victoriya" to R.drawable.dub_victoriya,
    "crunchyroll" to R.drawable.dub_crunchyroll,
    "anidub" to R.drawable.dub_anidub,
    "anidubonline" to R.drawable.dub_anidub,
    "studioband" to R.drawable.dub_studioband,
    "onwave" to R.drawable.dub_onwave,
    "subvost" to R.drawable.dub_subvost,
    "silveraniage" to R.drawable.dub_silver_aniage,
    "fumodub" to R.drawable.dub_fumodub,
    "dobrovoice" to R.drawable.dub_dobrovoice,
    "anibaza" to R.drawable.dub_anibaza,
    "freedub" to R.drawable.dub_freedub,
    "flowersmedia" to R.drawable.dub_flowers_media,
    "koekak" to R.drawable.dub_koekak,
    "leproduction" to R.drawable.dub_le_production,
    "anicore" to R.drawable.dub_anicore,
    "okane" to R.drawable.dub_okane,
    "dreamcast" to R.drawable.dub_dreamcast,
    "shizaproject" to R.drawable.dub_shiza,
    "shiza" to R.drawable.dub_shiza,
    "fronda" to R.drawable.dub_fronda,
    "frondastudio" to R.drawable.dub_fronda,
    "kazokuproject" to R.drawable.dub_kazoku,
    "amazingdubbing" to R.drawable.dub_amazing_dubbing,
    "anirise" to R.drawable.dub_anirise,
    "youmiteru" to R.drawable.dub_youmiteru,
    "комнатадиди" to R.drawable.dub_komnata_didi,
    "ушастаяозвучка" to R.drawable.dub_ushastaya_ozvuchka,
    "youkaistudio" to R.drawable.dub_youkai_studio,
    "пвашоу" to R.drawable.dub_pva_show,
    "animyruchime" to R.drawable.dub_ruchime,
    "ruchime" to R.drawable.dub_ruchime,
    "animy" to R.drawable.dub_animy,
    "тодубляжная" to R.drawable.dub_dublyajnaya,
    "anicosmic" to R.drawable.dub_anicosmic,
    "anirai" to R.drawable.dub_anirai,
    "jam" to R.drawable.dub_jam,
    "ruanime" to R.drawable.dub_ruanime,
    "руаниме" to R.drawable.dub_ruanime,
    "deep" to R.drawable.dub_deep,
)

/** Ключи внешних источников (отдельные модули каталога source): у них вместо логотипа — значок пазла. */
private val EXTERNAL_SOURCE_KEYS = setOf("anistar", "amedia", "aniboom", "animeboom", "anilibria", "4k", "anime4k")

/** Нормализованный ключ названия: без служебных префиксов («Озвучка », «Плеер »), регистра и знаков. */
internal fun String.logoKey(): String {
    var words = lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    while (words.size > 1 && words.first() in LOGO_LABEL_PREFIXES) words = words.drop(1)
    return words.joinToString("").filter { it.isLetterOrDigit() }
}

/** Логотип студии озвучки по названию из yani («Озвучка AniLibria»); `null`, если картинки нет. */
@DrawableRes
fun dubbingLogoRes(name: String): Int? = DUBBING_LOGOS[name.logoKey()]

/** У балансеров картинок нет — только значок; ресурс оставлен `null`, чтобы вызывающий код не менялся. */
@DrawableRes
@Suppress("UNUSED_PARAMETER")
fun balancerLogoRes(name: String): Int? = null

/** Значок балансера: пазл у внешних источников (Anistar, Amedia, AniBoom, 4K, AniLibria), иначе play. */
fun balancerFallbackIcon(name: String): ImageVector =
    if (name.logoKey() in EXTERNAL_SOURCE_KEYS) Icons.Filled.Extension else Icons.Filled.PlayCircle
