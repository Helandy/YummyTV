plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.library"
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
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:library:api"))
    implementation(project(":feature:library:presentation"))
    implementation(project(":feature:settings:api"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.bundles.coil.full)

    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.material.icons.core)

    ksp(libs.hilt.compiler)
}
