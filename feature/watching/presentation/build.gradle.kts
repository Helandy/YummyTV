plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.feature.watching.presentation"
}

dependencies {
    implementation(project(":core:storage"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:player:api"))

    implementation(libs.javax.inject)
    implementation(libs.jetbrains.navigation3.ui)
}
