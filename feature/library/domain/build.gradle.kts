plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":feature:account:domain"))
    implementation(libs.javax.inject)
}
