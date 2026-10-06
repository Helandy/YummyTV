plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Общие помощники для unit-тестов: подключается только через testImplementation.
dependencies {
    api(libs.junit)
    api(libs.mockk)
    api(libs.kotlinx.coroutines.test)
}
