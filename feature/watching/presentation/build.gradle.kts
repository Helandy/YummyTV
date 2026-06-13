plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "su.afk.yummy.tv.feature.watching.presentation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:storage"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:api"))

    implementation(libs.javax.inject)
    implementation(libs.jetbrains.navigation3.ui)
}
