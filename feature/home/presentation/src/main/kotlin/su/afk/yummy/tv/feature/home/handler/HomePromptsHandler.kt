package su.afk.yummy.tv.feature.home.handler

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.feature.home.model.PromptSchedule
import su.afk.yummy.tv.feature.home.utils.boostyPromptRemainingMs
import su.afk.yummy.tv.feature.home.utils.communityPromptRemainingMs
import su.afk.yummy.tv.feature.home.utils.supportPromptRemainingMs
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Сроки показа диалогов поддержки, сообщества и Boosty. Таймер каждого живёт внутри потока:
 * новый снимок настроек отменяет ожидание предыдущего.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class HomePromptsHandler @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    suspend fun ensureSupportInitialized() = settingsStore.ensureSupportPromptInstallTimeInitialized()

    suspend fun ensureCommunityInitialized() = settingsStore.ensureCommunityPromptFirstUseInitialized()

    fun supportSchedule(): Flow<PromptSchedule> =
        settingsStore.supportPromptSnapshot.flatMapLatest { snapshot ->
            schedule(snapshot.dismissed) { snapshot.supportPromptRemainingMs(nowMs = it) }
        }

    fun communitySchedule(): Flow<PromptSchedule> =
        settingsStore.communityPromptSnapshot.flatMapLatest { snapshot ->
            schedule(snapshot.dismissed) { snapshot.communityPromptRemainingMs(nowMs = it) }
        }

    fun boostySchedule(): Flow<PromptSchedule> =
        settingsStore.boostyPromptSnapshot.flatMapLatest { snapshot ->
            schedule(snapshot.dismissed) { snapshot.boostyPromptRemainingMs(nowMs = it) }
        }

    /** Помечает диалог отработавшим, чтобы больше не показывать. */
    suspend fun markSupportHandled() = settingsStore.dismissSupportPrompt()

    suspend fun markCommunityHandled() = settingsStore.dismissCommunityPrompt()

    suspend fun markBoostyHandled() = settingsStore.dismissBoostyPrompt()

    private fun schedule(dismissed: Boolean, remainingMs: (nowMs: Long) -> Long): Flow<PromptSchedule> = flow {
        if (dismissed) {
            emit(PromptSchedule.Dismissed)
            return@flow
        }
        val remaining = remainingMs(System.currentTimeMillis())
        if (remaining > 0L) {
            emit(PromptSchedule.Waiting)
            delay(remaining.milliseconds)
        }
        emit(PromptSchedule.Due)
    }
}
