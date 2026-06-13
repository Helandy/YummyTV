plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinSerialization)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
