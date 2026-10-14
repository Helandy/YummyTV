plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Только контракты хранилищ Yani (токен, учётка): чтобы core:network не тянул весь core:preferences.
dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
}
