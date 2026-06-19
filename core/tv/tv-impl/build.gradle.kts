plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.tv"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:tv:tv-api"))
    implementation(project(":core:utils"))
    implementation(project(":feature:home:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tvprovider)
}
