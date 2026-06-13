plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.feature.main.mobile"
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
    implementation(project(":core:update"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:home:ui-mobile"))
    implementation(project(":feature:library:ui-mobile"))
    implementation(project(":feature:main:api"))
    implementation(project(":feature:main:presentation"))
    implementation(project(":feature:schedule:ui-mobile"))
    implementation(project(":feature:search:ui-mobile"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:top:ui-mobile"))

    implementation(libs.bundles.compose.core)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    ksp(libs.hilt.compiler)
}
