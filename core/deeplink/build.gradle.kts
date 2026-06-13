plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.deeplink"
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":feature:details:api"))
    implementation(libs.jetbrains.navigation3.ui)
}
