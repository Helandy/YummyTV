package su.afk.yummy.tv.source.anime4k.data.ads

import android.content.Context
import su.afk.yummy.tv.core.designsystem.dialog.ConfirmDialogCenter
import su.afk.yummy.tv.source.anime4k.data.R

/** Вопрос про рекламу рисуется общим диалогом приложения (Material 3, текущая тема). */
internal object AdConsentDialog {

    /** `true`, если пользователь согласился посмотреть рекламу; без хоста на экране `false`. */
    suspend fun ask(context: Context): Boolean = ConfirmDialogCenter.ask(
        title = context.getString(R.string.anime4k_ad_dialog_title),
        message = context.getString(R.string.anime4k_ad_dialog_message),
        confirmText = context.getString(R.string.anime4k_ad_dialog_confirm),
        dismissText = context.getString(R.string.anime4k_ad_dialog_cancel),
    )
}
