plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.baselineprofile)
    id("yummytv.baselineprofile.tasks")
}

android {
    namespace = "su.afk.yummy.tv.baselineprofile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // генерация профиля доступна с API 28
        minSdk = 28
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // бенчмарки гоняются на эмуляторах: абсолютные цифры шумные, значима разница режимов
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    targetProjectPath = ":app"
}

// Профиль собирается на всех подключённых устройствах сразу: телефон + Android TV,
// генераторы сами пропускают «чужой» тип устройства.
baselineProfile {
    useConnectedDevices = true
}

// com.android.test-модуль инструментирует сам себя, поэтому id целевого приложения передаём
// аргументом раннера; у releaseDebug он с суффиксом (см. app/build.gradle.kts).
// В benchmark-вариантах гоняем только пакет benchmark (при генерации плагин сам включает
// только BaselineProfileRule).
val baseApplicationId = providers.gradleProperty("yummytv.applicationId").get()
androidComponents {
    onVariants { variant ->
        val suffix = if (variant.name.endsWith("ReleaseDebug")) ".releasedebug" else ""
        variant.instrumentationRunnerArguments.put("targetAppId", baseApplicationId + suffix)
        if (variant.name.startsWith("benchmark")) {
            variant.instrumentationRunnerArguments.put("androidx.benchmark.enabledRules", "Macrobenchmark")
            // иначе генераторы попадут в прогон и отчитаются «падением» от Assume
            variant.instrumentationRunnerArguments.put("package", "su.afk.yummy.tv.baselineprofile.benchmark")
        }
    }
}

dependencies {
    implementation(libs.androidx.testExt.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
