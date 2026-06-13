plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.account.mobile"
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
    implementation(project(":feature:account:api"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:account:presentation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)

    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.hilt.android)

    ksp(libs.hilt.compiler)
}
