plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.schedule"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:schedule:api"))
    implementation(project(":feature:schedule:domain"))
    implementation(project(":feature:schedule:presentation"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.navigation.serialization)

    implementation(libs.coil.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)

    ksp(libs.hilt.compiler)
}
