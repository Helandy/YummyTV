package su.afk.yummy.tv.data.update.apk

import androidx.core.content.FileProvider
import su.afk.yummy.tv.data.update.R

/**
 * Отдаёт загруженный APK установщику прошивки. Свой подкласс нужен, чтобы не конфликтовать
 * в манифесте с FileProvider логов: два провайдера одного класса не сливаются.
 */
internal class UpdateApkFileProvider : FileProvider(R.xml.update_apk_file_paths) {
    companion object {
        fun authority(packageName: String) = "$packageName.update"
    }
}
