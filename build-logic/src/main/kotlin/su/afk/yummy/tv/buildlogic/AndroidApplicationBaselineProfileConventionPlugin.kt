package su.afk.yummy.tv.buildlogic

import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * App-сторона baseline profile: подключает `androidx.baselineprofile` и настраивает его под
 * ручную генерацию профиля (задачи генерации — в [BaselineProfileTasksPlugin]).
 */
class AndroidApplicationBaselineProfileConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("androidx.baselineprofile")

        // Плагин создаёт nonMinifiedRelease/benchmarkRelease на основе release, а у release своей
        // подписи нет — без debug-ключа эти APK не поставить на устройство.
        extensions.configure<ApplicationExtension> {
            buildTypes.matching { it.name.startsWith("nonMinified") || it.name.startsWith("benchmark") }
                .configureEach { signingConfig = signingConfigs.getByName("debug") }
        }

        // Профиль генерируется вручную на эмуляторах (см. docs/baseline-profile.md) и коммитится
        // в src/release/generated/baselineProfiles; обычная сборка его только упаковывает.
        extensions.configure<BaselineProfileConsumerExtension> {
            automaticGenerationDuringBuild = false
            dexLayoutOptimization = true
        }

        // -Pyummytv.profile.dexLayout=false выключает раскладку DEX у benchmark-сборки, чтобы
        // бенчмарками померить её вклад (docs/baseline-profile.md). Через свойство AGP, а не
        // dexLayoutOptimization: плагин настраивает только release/releaseDebug, benchmarkRelease
        // берёт дефолт AGP.
        val benchmarkDexLayout = providers.gradleProperty("yummytv.profile.dexLayout").orNull != "false"
        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                if (!benchmarkDexLayout && variant.buildType.orEmpty().startsWith("benchmark")) {
                    variant.experimentalProperties.put("android.experimental.r8.dex-startup-optimization", false)
                }
            }
        }
    }
}
