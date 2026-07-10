plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.data.player"
}

dependencies {
    implementation(project(":core:logger"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
}
