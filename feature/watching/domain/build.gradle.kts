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
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.javax.inject)
}
