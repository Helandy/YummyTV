package su.afk.yummy.tv.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            compileSdk = libs.versionInt("android-compileSdk")
            defaultConfig {
                minSdk = libs.versionInt("android-minSdk")
            }
            configureJava21()
        }
        addCoreLibraryDesugaring()
        enforceLayering()
    }
}

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("yummytv.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("com.github.skydoves.compose.stability.analyzer")
        configureComposeCompiler()
        addComposeBom()
        dependencies.add("implementation", libs.findLibrary("compose-uiToolingPreview").get())
        dependencies.add("debugImplementation", libs.findLibrary("compose-uiTooling").get())
        dependencies.add("api", libs.findLibrary("kotlinx-collections-immutable").get())
        Unit
    }
}

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("com.github.skydoves.compose.stability.analyzer")
        configureComposeCompiler()
        addComposeBom()
        extensions.configure<ApplicationExtension> {
            compileSdk = libs.versionInt("android-compileSdk")
            defaultConfig {
                minSdk = libs.versionInt("android-minSdk")
            }
            configureJava21()
        }
        addCoreLibraryDesugaring()
    }
}

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val kotlinMetadataJvmVersion =
                libs.findVersion("kotlin-metadata-jvm").get().requiredVersion
            configurations.configureEach {
                resolutionStrategy.eachDependency {
                    if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-metadata-jvm") {
                        useVersion(kotlinMetadataJvmVersion)
                        because("Hilt 2.59.2 depends on kotlin-metadata-jvm 2.2.20, which cannot read Kotlin 2.3 metadata.")
                    }
                }
            }
            pluginManager.apply("com.google.dagger.hilt.android")
            pluginManager.apply("com.google.devtools.ksp")
            dependencies.add("implementation", libs.findLibrary("hilt-android").get())
            dependencies.add("ksp", libs.findLibrary("hilt-compiler").get())
        }
    }
}

private val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

private fun VersionCatalog.versionInt(name: String): Int =
    findVersion(name).get().requiredVersion.toInt()

private fun LibraryExtension.configureJava21() {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
}

private fun ApplicationExtension.configureJava21() {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
}

/**
 * Направление зависимостей между слоями: `core:` — фундамент, он не знает про фичи.
 * Обратная зависимость `core:* -> feature:*` означает, что порт объявлен не на той стороне —
 * контракт должен принадлежать core-модулю, а реализация жить в фиче (см. NavRegistrar).
 *
 * `-PstrictLayering=false` временно понижает нарушение до предупреждения.
 */
private fun Project.enforceLayering() {
    val consumer = path
    if (!consumer.startsWith(":core:")) return
    val strict = providers.gradleProperty("strictLayering").orNull != "false"
    val log = logger
    configurations.configureEach {
        dependencies.whenObjectAdded {
            val target = (this as? ProjectDependency)?.path
            if (target != null && target.startsWith(":feature:")) {
                val message = "Нарушение слоёв: $consumer зависит от $target. " +
                    "core-модули не должны знать про feature-модули — объявите порт " +
                    "в core и реализуйте его в фиче."
                if (strict) throw GradleException(message) else log.warn("w: $message")
            }
        }
    }
}

// Единственный источник версий Compose: артефакты в каталоге объявлены без версий,
// их проставляет androidx.compose:compose-bom. debugImplementation не наследует implementation,
// но debug-вариант резолвит обе конфигурации одним classpath, поэтому ui-tooling тоже покрыт.
private fun Project.addComposeBom() {
    val bom = dependencies.platform(libs.findLibrary("androidx-compose-bom").get())
    dependencies.add("implementation", bom)
    dependencies.add("androidTestImplementation", bom)
}

private fun Project.addCoreLibraryDesugaring() {
    dependencies.add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
}

// Метрики/отчёты стабильности Compose: -PenableComposeCompilerReports=true.
// providers.gradleProperty + isolated.rootProject — ради configuration cache и project isolation.
private fun Project.configureComposeCompiler() {
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        if (providers.gradleProperty("enableComposeCompilerReports").orNull == "true") {
            val outputDir = layout.buildDirectory.dir("compose_compiler")
            metricsDestination.set(outputDir)
            reportsDestination.set(outputDir)
        }
        val stabilityFile =
            isolated.rootProject.projectDirectory.file("config/compose-stability.conf")
        if (stabilityFile.asFile.exists()) {
            stabilityConfigurationFiles.add(stabilityFile)
        }
    }
}
