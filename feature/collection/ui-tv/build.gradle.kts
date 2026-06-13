plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.collection"
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
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:collection:presentation"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:settings:api"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.bundles.coil.full)

    implementation(libs.hilt.android)
    implementation(libs.androidx.material.icons.core)

    ksp(libs.hilt.compiler)
}
