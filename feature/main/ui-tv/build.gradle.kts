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
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:update"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:home:ui-tv"))
    implementation(project(":feature:library:ui-tv"))
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:presentation"))
    implementation(project(":feature:schedule:ui-tv"))
    implementation(project(":feature:search:ui-tv"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:top:ui-tv"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.navigation.serialization)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.material.icons.core)

    ksp(libs.hilt.compiler)
}
