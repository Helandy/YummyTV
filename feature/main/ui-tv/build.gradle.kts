plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.feature.main"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:presentation"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:storage"))
    implementation(project(":core:preferences"))
    implementation(project(":core:update"))

    implementation(project(":feature:settings:api"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:home:ui-tv"))
    implementation(project(":feature:search:ui-tv"))
    implementation(project(":feature:schedule:ui-tv"))
    implementation(project(":feature:top:ui-tv"))
    implementation(project(":feature:library:ui-tv"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.androidx.material.icons.core)
}
