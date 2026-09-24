plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.update.presentation"
}

dependencies {
    api(project(":core:mvi"))
    implementation(project(":core:utils"))
    api(project(":feature:update:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:update:api"))

    implementation(libs.kotlinx.coroutines.android)
}
