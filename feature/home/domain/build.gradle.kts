plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:library:domain"))
    implementation(project(":feature:schedule:domain"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
}
