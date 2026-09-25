package su.afk.yummy.tv.buildlogic

import com.android.build.api.variant.TestAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

private const val TASK_GROUP = "yummytv profile"
private const val DEFAULT_AVDS = "37_Pixel_10_Pro_XL,14_Android_TV_1080p"

/**
 * Задачи модуля `:baselineprofile`, которые сами поднимают эмуляторы:
 * - `generateBaselineProfiles` — эмуляторы → генерация профиля в `app/src/release/generated` → гашение;
 * - `runProfileBenchmarks` — эмуляторы → бенчмарки «без профиля / с профилем» → отчёт → гашение.
 *   С `-Pyummytv.profile.dexLayout=false` тестируемый APK собирается без раскладки DEX,
 *   отчёт пишется в отдельный файл — так вклад раскладки сравнивается с обычным прогоном.
 *
 * Gradle Managed Devices не годятся: они не умеют образы android-tv, поэтому используются
 * локальные AVD из свойства `yummytv.profile.avds` (через запятую; `-P` или gradle.properties).
 * Уже запущенные эмуляторы и подключённые устройства переиспользуются и не гасятся.
 */
class BaselineProfileTasksPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val sdkDirectory = extensions.getByType<TestAndroidComponentsExtension>()
            .sdkComponents.sdkDirectory
        val script = isolated.rootProject.projectDirectory.file("scripts/profile-emulators.sh")
        val stateFile = layout.buildDirectory.file("profile-emulators/started.txt")
        val avds = providers.gradleProperty("yummytv.profile.avds").orElse(DEFAULT_AVDS)
            .map { value -> value.split(',').map(String::trim).filter(String::isNotEmpty) }
        val dexLayout = providers.gradleProperty("yummytv.profile.dexLayout").map { it != "false" }.orElse(true)

        val startEmulators = tasks.register<Exec>("startProfileEmulators") {
            group = TASK_GROUP
            description = "Поднимает AVD из yummytv.profile.avds и ждёт их загрузки"
            environment("ANDROID_SDK_ROOT", sdkDirectory.get().asFile.absolutePath)
            executable("sh")
            argumentProviders.add {
                listOf(script.asFile.absolutePath, "start", stateFile.get().asFile.absolutePath) + avds.get()
            }
            outputs.upToDateWhen { false }
        }
        val stopEmulators = tasks.register<Exec>("stopProfileEmulators") {
            group = TASK_GROUP
            description = "Гасит эмуляторы, поднятые startProfileEmulators"
            environment("ANDROID_SDK_ROOT", sdkDirectory.get().asFile.absolutePath)
            executable("sh")
            argumentProviders.add {
                listOf(script.asFile.absolutePath, "stop", stateFile.get().asFile.absolutePath)
            }
            outputs.upToDateWhen { false }
        }

        // все задачи, которые ходят на устройства, — только после подъёма эмуляторов
        tasks.matching { it.name.startsWith("connected") }.configureEach {
            mustRunAfter(startEmulators)
        }
        stopEmulators.configure {
            mustRunAfter(tasks.matching { it.name.startsWith("connected") })
        }

        val benchmarkReport = tasks.register<BenchmarkReportTask>("benchmarkReport") {
            group = TASK_GROUP
            description = "Сводит результаты бенчмарков в таблицу «без профиля / с профилем»"
            resultsDirectory.set(
                layout.buildDirectory.dir("outputs/connected_android_test_additional_output"),
            )
            dexLayoutEnabled.set(dexLayout)
            reportFile.set(
                dexLayout.flatMap { enabled ->
                    val suffix = if (enabled) "" else "-no-dex-layout"
                    layout.buildDirectory.file("reports/baseline-profile-benchmark$suffix.md")
                },
            )
            mustRunAfter(tasks.matching { it.name.startsWith("connected") })
        }
        stopEmulators.configure { mustRunAfter(benchmarkReport) }

        // Финализаторы висят на самих задачах, а не на агрегирующих: агрегирующая задача
        // не выполняется, если упала её зависимость, и эмуляторы остались бы висеть.
        startEmulators.configure { finalizedBy(stopEmulators) }
        tasks.matching { it.name == "connectedBenchmarkReleaseAndroidTest" }.configureEach {
            finalizedBy(benchmarkReport)
        }

        tasks.register("generateBaselineProfiles") {
            group = TASK_GROUP
            description = "Поднимает эмуляторы и генерирует baseline/startup-профиль для release"
            dependsOn(startEmulators, ":app:generateReleaseBaselineProfile")
        }

        tasks.register("runProfileBenchmarks") {
            group = TASK_GROUP
            description = "Поднимает эмуляторы и сравнивает старт/кадры без профиля и с профилем"
            dependsOn(startEmulators, "connectedBenchmarkReleaseAndroidTest")
        }
        Unit
    }
}
