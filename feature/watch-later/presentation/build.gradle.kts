plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.watchlater.presentation"
}

dependencies {
    implementation(project(":feature:watch-later:api"))
    implementation(project(":core:error:api"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:watch-later:domain"))

    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.kotlinx.coroutines.android)
}
