plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.feature.library.presentation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:storage"))
    implementation(project(":core:preferences"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:details:api"))
    api(project(":feature:details:domain"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:account:domain"))
    implementation(libs.compose.runtime)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
