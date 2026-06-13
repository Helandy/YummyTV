plugins {
    id("yummytv.android.library.compose")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.schedule.presentation"
}

dependencies {
    api(project(":feature:schedule:domain"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:details:api"))

    implementation(libs.bundles.compose.presentation)
}
