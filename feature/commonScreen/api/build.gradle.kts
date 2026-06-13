plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.feature.commonscreen.api"
}

dependencies {
    implementation(project(":core:model"))

    api(libs.jetbrains.navigation3.ui)
}
