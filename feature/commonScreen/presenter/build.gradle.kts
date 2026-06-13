plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.feature.commonscreen"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:commonScreen:api"))

    implementation(libs.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.lifecycle.viewmodelCompose)

    ksp(libs.hilt.compiler)
}
