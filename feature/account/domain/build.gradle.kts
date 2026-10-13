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
    implementation(libs.javax.inject)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test)
}
