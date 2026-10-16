plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.catalog.android"
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":feature:catalog:api"))
    implementation(project(":feature:catalog:domain"))

    implementation(libs.kotlinx.coroutines.android)
}
