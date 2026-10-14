plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Чистые Kotlin-хелперы без Android: их могут брать JVM-domain модули.
// Пакеты — core.common.*, core:utils ре-экспортирует модуль через api.
dependencies {
    api(libs.kotlinx.coroutines.core)
}
