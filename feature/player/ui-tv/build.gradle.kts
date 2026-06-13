plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.player"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:player:presentation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.bundles.media3.player.dash)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.hilt.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    ksp(libs.hilt.compiler)
}
