package su.afk.yummy.tv.data.update.apk

import androidx.core.content.FileProvider

/**
 * Отдаёт загруженный APK установщику прошивки. Свой подкласс нужен, чтобы не конфликтовать
 * в манифесте с FileProvider логов: два провайдера одного класса не сливаются.
 *
 * Пути берутся из `meta-data` в манифесте, а не из конструктора с ресурсом: статический
 * `FileProvider.getUriForFile` читает только манифест и иначе падает «Missing FILE_PROVIDER_PATHS».
 */
internal class UpdateApkFileProvider : FileProvider() {
    companion object {
        fun authority(packageName: String) = "$packageName.update"
    }
}
