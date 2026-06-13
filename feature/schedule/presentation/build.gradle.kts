plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.feature.schedule.presentation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":feature:schedule:domain"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:details:api"))

    implementation(libs.bundles.compose.presentation)

    implementation(libs.hilt.android)

    ksp(libs.hilt.compiler)
}
