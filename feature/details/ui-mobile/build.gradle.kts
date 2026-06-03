plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.details.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:details:presentation"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:collection:api"))
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
}
