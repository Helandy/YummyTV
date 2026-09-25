plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Чистые Kotlin-хелперы без Android: их могут брать JVM-domain модули.
// Пакеты оставлены прежними (core.utils.*), core:utils ре-экспортирует модуль через api.
dependencies {
    api(libs.kotlinx.coroutines.core)
}
