plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.designsystem"
}

dependencies {
    api(project(":core:model"))

    implementation(project(":core:error"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.coil.full)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.kotlinx.serialization.json)
}
