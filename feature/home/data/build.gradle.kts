plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.data.home"
}

dependencies {
    implementation(project(":core:error"))
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:home:domain"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.unit.test.network)
}
