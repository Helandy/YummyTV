package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.core.model.settings.BoostyPromptSnapshot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.model.settings.CommunityPromptSnapshot
import su.afk.yummy.tv.core.model.settings.HomeBlockConfig
import su.afk.yummy.tv.core.model.settings.HomeBlockKind
import su.afk.yummy.tv.core.model.settings.SupportPromptSnapshot
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.utils.isEpisodeWatched
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Включённые блоки главной в порядке настроек. */
internal fun List<HomeBlockConfig>.enabledKinds(): ImmutableList<HomeBlockKind> =
    filter { it.enabled }.map { it.kind }.toImmutableList()

/** Тип секции ленты, которую рисует блок; `null` для блоков, не являющихся секциями ленты. */
fun HomeBlockKind.toSectionType(): HomeFeedSectionType? = when (this) {
    HomeBlockKind.MY_NEW_EPISODES -> HomeFeedSectionType.MY_NEW_EPISODES
    HomeBlockKind.NEW_RELEASES -> HomeFeedSectionType.NEW_RELEASES
    HomeBlockKind.NEW_TITLES -> HomeFeedSectionType.NEW_TITLES
    HomeBlockKind.RECOMMENDATIONS -> HomeFeedSectionType.RECOMMENDATIONS
    HomeBlockKind.ANNOUNCEMENTS -> HomeFeedSectionType.ANNOUNCEMENTS
    HomeBlockKind.COLLECTIONS -> HomeFeedSectionType.COLLECTIONS
    HomeBlockKind.HERO,
    HomeBlockKind.CONTINUE_WATCHING,
    HomeBlockKind.BLOGGER_VIDEOS,
    HomeBlockKind.QUICK_ACTIONS,
    -> null
}

/** Убирает из блока рекомендаций тайтлы, скрытые пользователем. */
internal fun HomeFeed.withoutHiddenRecommendations(hiddenIds: Set<Int>): HomeFeed {
    if (hiddenIds.isEmpty()) return this
    return copy(
        sections = sections.map { section ->
            if (section.type == HomeFeedSectionType.RECOMMENDATIONS) {
                section.copy(items = section.items.filterNot { it.id in hiddenIds })
            } else {
                section
            }
        },
    )
}

/** Перемешивает тайтлы «Аниме сезона»; один и тот же [random]-seed даёт один и тот же порядок. */
internal fun HomeFeed.withShuffledHero(random: Random): HomeFeed =
    if (heroItems.size < 2) this else copy(heroItems = heroItems.shuffled(random))

/**
 * Добавляет в начало ленты секцию недавно вышедших серий тайтлов из [libraryAnimeIds]; без
 * совпадений секцию не добавляет. Серии из [watchedEpisodes] убираются из секции.
 */
internal fun HomeFeed.withMyNewEpisodes(
    recentlyAired: List<HomeFeedItem>,
    libraryAnimeIds: Set<Int>,
    watchedEpisodes: Map<Int, Set<Int>>,
    title: String,
): HomeFeed {
    val items = recentlyAired
        .filter { it.id in libraryAnimeIds }
        .map { item -> item.copy(isWatched = item.isEpisodeWatched(watchedEpisodes)) }
        .filterNot { it.isWatched }
    if (items.isEmpty()) return this
    val section = HomeFeedSection(HomeFeedSectionType.MY_NEW_EPISODES, title, items)
    return copy(sections = listOf(section) + sections)
}

internal fun HomeContinueWatchingItem.hasPlayableTarget(): Boolean =
    videoId > 0 || episode.isNotBlank() || episodeUrl.isNotBlank()

private val SUPPORT_PROMPT_DELAY_MS: Long = TimeUnit.DAYS.toMillis(7)

/** Сколько ещё ждать до показа окна поддержки: неделя с момента, когда оно стало доступно. */
internal fun SupportPromptSnapshot.supportPromptRemainingMs(nowMs: Long): Long =
    SUPPORT_PROMPT_DELAY_MS - (nowMs - firstEligibleTimeMs)

private val COMMUNITY_PROMPT_DELAY_MS: Long = TimeUnit.DAYS.toMillis(2)

/** Сколько ещё ждать до приглашения в сообщество: 2 дня с первого использования. */
internal fun CommunityPromptSnapshot.communityPromptRemainingMs(nowMs: Long): Long =
    COMMUNITY_PROMPT_DELAY_MS - (nowMs - firstUseTimeMs)

private val BOOSTY_PROMPT_DELAY_MS: Long = TimeUnit.DAYS.toMillis(14)

/** Сколько ещё ждать до предложения поддержать на Boosty: 14 дней с реальной установки. */
internal fun BoostyPromptSnapshot.boostyPromptRemainingMs(nowMs: Long): Long =
    BOOSTY_PROMPT_DELAY_MS - (nowMs - installTimeMs)

/**
 * Предложение добавить preview-канал показывается один раз на телевизоре, когда пользователь уже
 * смотрел что-то, канал ещё не добавлен, а поверх главной нет других диалогов.
 */
fun shouldOfferPreviewChannel(
    isSupported: Boolean,
    answered: Boolean,
    browsable: Boolean,
    hasContinueWatching: Boolean,
    otherDialogVisible: Boolean,
): Boolean = isSupported && !answered && !browsable && hasContinueWatching && !otherDialogVisible
