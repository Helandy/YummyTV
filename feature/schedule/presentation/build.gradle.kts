plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.schedule.presentation"
}

dependencies {
    implementation(project(":feature:schedule:api"))
    implementation(project(":core:utils"))
    api(project(":feature:schedule:domain"))

    implementation(project(":core:analytics"))
    implementation(project(":core:error:api"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:details:api"))

    implementation(libs.bundles.compose.presentation)
}
