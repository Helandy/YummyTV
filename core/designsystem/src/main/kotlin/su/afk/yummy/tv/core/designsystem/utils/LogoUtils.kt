package su.afk.yummy.tv.core.designsystem.utils

import androidx.annotation.DrawableRes
import su.afk.yummy.tv.core.designsystem.R

private val LOGO_LABEL_PREFIXES = listOf("плеер", "player", "озвучка", "субтитры", "дубляж")

private val DUBBING_LOGOS: Map<String, Int> = mapOf(
    "anistar" to R.drawable.dub_anistar,
    "animevost" to R.drawable.dub_animevost,
    "anilibria" to R.drawable.dub_anilibria,
    "animaunt" to R.drawable.dub_animaunt,
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

private val BALANCER_LOGOS: Map<String, Int> = mapOf(
    "vk" to R.drawable.balancer_vk,
    "vkvideo" to R.drawable.balancer_vk,
    "rutube" to R.drawable.balancer_rutube,
    "anilibria" to R.drawable.dub_anilibria,
    "anistar" to R.drawable.dub_anistar,
    "ruanime" to R.drawable.dub_ruanime,
    "руаниме" to R.drawable.dub_ruanime,
    "deep" to R.drawable.dub_deep,
)

/** Нормализованный ключ названия: без служебных префиксов («Озвучка », «Плеер »), регистра и знаков. */
internal fun String.logoKey(): String {
    var words = lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    while (words.size > 1 && words.first() in LOGO_LABEL_PREFIXES) words = words.drop(1)
    return words.joinToString("").filter { it.isLetterOrDigit() }
}

/** Логотип студии озвучки по названию из yani («Озвучка AniLibria»); `null`, если картинки нет. */
@DrawableRes
fun dubbingLogoRes(name: String): Int? = DUBBING_LOGOS[name.logoKey()]

/** Логотип балансера по названию («Плеер Kodik»); `null` для тех, у кого картинки нет. */
@DrawableRes
fun balancerLogoRes(name: String): Int? = BALANCER_LOGOS[name.logoKey()]
