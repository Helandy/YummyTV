plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.videodownload.presentation"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:video-download:domain"))

    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.kotlinx.coroutines.android)
}
