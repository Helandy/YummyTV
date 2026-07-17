plugins {
    `kotlin-dsl`
}

group = "su.afk.yummy.tv.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.hilt.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "yummytv.android.library"
            implementationClass = "su.afk.yummy.tv.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "yummytv.android.library.compose"
            implementationClass = "su.afk.yummy.tv.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "yummytv.android.application"
            implementationClass = "su.afk.yummy.tv.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidHilt") {
            id = "yummytv.android.hilt"
            implementationClass = "su.afk.yummy.tv.buildlogic.AndroidHiltConventionPlugin"
        }
    }
}
