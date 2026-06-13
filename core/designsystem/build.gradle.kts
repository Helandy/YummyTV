plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.core.designsystem"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:model"))

    implementation(project(":core:error"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.coil.full)

    implementation(libs.hilt.android)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.kotlinx.serialization.json)

    ksp(libs.hilt.compiler)
}
