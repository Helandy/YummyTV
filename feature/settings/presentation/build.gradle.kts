plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.settings.presentation"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:tv:tv-api"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:video-download:domain"))

    implementation(libs.androidx.lifecycle.viewmodelCompose)
}
