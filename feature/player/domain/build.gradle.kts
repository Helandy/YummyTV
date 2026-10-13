plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.javax.inject)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test)
}
