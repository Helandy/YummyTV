plugins {
    id("yummytv.android.library.compose")
}

android {
    namespace = "su.afk.yummy.tv.core.designsystem"
}

dependencies {
    api(project(":core:model"))

    implementation(project(":core:mvi"))
    implementation(project(":core:utils"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.coil.full)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.kotlinx.serialization.json)
}
